import copy
import os
import unittest
from rates_server import validate_snapshot, MySqlStore, DatabaseError, make_handler


def sample(date="2026-11-09"):
    return {"update_date": date, "source_url": "https://chl.cn/huilv/?jinri",
            "rows": [{"currency_name": "美元", "quote": "700"},
                     {"currency_name": "欧元", "quote": "800"},
                     {"currency_name": "韩国元", "quote": "0.5"},
                     {"currency_name": "英镑", "quote": "900"}]}


class ValidationTest(unittest.TestCase):
    def test_rejects_duplicate_missing_and_invalid_quotes(self):
        for mutate in [lambda x: x["rows"].append(x["rows"][0]),
                       lambda x: x["rows"].pop(0),
                       lambda x: x["rows"][0].update(quote="0"),
                       lambda x: x["rows"][0].update(quote="NaN"),
                       lambda x: x.update(update_date="2026-02-30")]:
            value = sample()
            mutate(value)
            with self.assertRaises(ValueError):
                validate_snapshot(value)

    def test_retains_exact_currency_names_and_decimal_quote(self):
        value = sample()
        value["rows"].append({"currency_name": "O'Reilly\\币", "quote": "123.45678901"})
        result = validate_snapshot(value)
        self.assertEqual("O'Reilly\\币", result["rows"][-1]["currency_name"])
        self.assertEqual("123.45678901", result["rows"][-1]["quote"])


@unittest.skipUnless(os.environ.get("MYSQL_INTEGRATION") == "1", "requires local MySQL login")
class MySqlTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        # A new, isolated database, never the user's existing schemas.
        cls.store = MySqlStore(database="rates_test_" + str(os.getpid()))
        if cls.store.execute("SHOW DATABASES LIKE '" + cls.store.database + "';", with_database=False).strip():
            raise RuntimeError("Test database already exists; refusing to reuse it")
        cls.store.initialize()

    @classmethod
    def tearDownClass(cls):
        cls.store.execute("DROP DATABASE `" + cls.store.database + "`;", with_database=False)

    def test_snapshot_replacement_history_and_rollback(self):
        store = self.store
        store.save(sample("2026-11-08"))
        current = sample()
        current["rows"].append({"currency_name": "O'Reilly\\币", "quote": "12.3456"})
        store.save(current)
        data = store.read()
        self.assertEqual("2026-11-09", data["update_date"])
        self.assertEqual("O'Reilly\\币", data["rows"][-1]["currency_name"])
        self.assertEqual("12.3456", data["rows"][-1]["quote"])
        current["rows"].pop()
        current["rows"][0]["quote"] = "701"
        store.save(current)
        self.assertEqual(4, len(store.read()["rows"]))
        self.assertEqual("701", store.read()["rows"][0]["quote"])
        self.assertEqual("2", store.execute("SELECT COUNT(*) FROM rate_snapshots;").strip())
        with self.assertRaises(DatabaseError):
            store.execute("START TRANSACTION; UPDATE currency_rates SET quote=1; SELECT * FROM no_such_table; COMMIT;")
        self.assertEqual("701", store.read()["rows"][0]["quote"])
        invalid = copy.deepcopy(current)
        invalid["rows"][0]["quote"] = "-1"
        with self.assertRaises(ValueError):
            store.save(invalid)
        self.assertEqual("701", store.read()["rows"][0]["quote"])

    def test_http_persists_and_queries_real_mysql_and_rejects_browser_writes(self):
        import json
        import threading
        import urllib.request
        import urllib.error
        from http.server import ThreadingHTTPServer
        server = ThreadingHTTPServer(("127.0.0.1", 0), make_handler(self.store))
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        url = "http://127.0.0.1:" + str(server.server_port)
        try:
            body = json.dumps(sample(), ensure_ascii=False).encode("utf-8")
            request = urllib.request.Request(url + "/api/rates", body, {"Content-Type": "application/json"})
            with urllib.request.urlopen(request) as response:
                self.assertEqual(4, json.load(response)["count"])
            with urllib.request.urlopen(url + "/api/rates") as response:
                self.assertEqual("700", json.load(response)["rows"][0]["quote"])
            with urllib.request.urlopen(url + "/database") as response:
                self.assertIn("美元", response.read().decode("utf-8"))
            request.add_header("Origin", "https://unrelated.example")
            with self.assertRaises(urllib.error.HTTPError) as failure:
                urllib.request.urlopen(request)
            self.assertEqual(403, failure.exception.code)
        finally:
            server.shutdown()
            server.server_close()
            thread.join()


if __name__ == "__main__":
    unittest.main()

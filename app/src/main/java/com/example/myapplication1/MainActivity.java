package com.example.myapplication1;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private EditText heightInput;
    private EditText weightInput;
    private TextView resultText;
    private TextView adviceText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        heightInput = findViewById(R.id.height_input);
        weightInput = findViewById(R.id.weight_input);
        resultText = findViewById(R.id.result_text);
        adviceText = findViewById(R.id.advice_text);

        Button calculateButton = findViewById(R.id.calculate_button);
        calculateButton.setOnClickListener(v -> calculateBmi());
    }

    private void calculateBmi() {
        String heightText = heightInput.getText().toString().trim();
        String weightText = weightInput.getText().toString().trim();

        try {
            double height = Double.parseDouble(heightText);
            double weight = Double.parseDouble(weightText);

            if (height <= 0 || weight <= 0) {
                showInputError();
                return;
            }

            double heightInMeters = height / 100;
            double bmi = weight / (heightInMeters * heightInMeters);
            String bmiValue = String.format(Locale.CHINA, "%.2f", bmi);
            resultText.setText(getString(R.string.bmi_result, bmiValue));
            adviceText.setText(getString(R.string.health_advice, getAdvice(bmi)));
        } catch (NumberFormatException e) {
            showInputError();
        }
    }

    private String getAdvice(double bmi) {
        if (bmi < 18.5) {
            return getString(R.string.advice_underweight);
        } else if (bmi < 24) {
            return getString(R.string.advice_normal);
        } else if (bmi < 28) {
            return getString(R.string.advice_overweight);
        } else {
            return getString(R.string.advice_obese);
        }
    }

    private void showInputError() {
        resultText.setText(R.string.invalid_bmi_input);
        adviceText.setText(R.string.health_advice_default);
    }
}

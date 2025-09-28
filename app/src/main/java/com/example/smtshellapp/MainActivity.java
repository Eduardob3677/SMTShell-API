package com.example.smtshellapp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.blufenix.smtshell.api.SMTShellAPI;

public class MainActivity extends AppCompatActivity {

    private TextView textViewOutput;
    private Button buttonRequestRoot;
    private Button buttonExecuteCommand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        textViewOutput = findViewById(R.id.textViewOutput);
        buttonRequestRoot = findViewById(R.id.buttonRequestRoot);
        buttonExecuteCommand = findViewById(R.id.buttonExecuteCommand);
        
        buttonRequestRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestRootAccess();
            }
        });
        
        buttonExecuteCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                executeTestCommand();
            }
        });
        
        // Check if root is available
        if (SMTShellAPI.isRootAvailable()) {
            textViewOutput.setText("Root access is available on this device");
            buttonExecuteCommand.setEnabled(true);
        } else {
            textViewOutput.setText("Root access is not available on this device");
            buttonExecuteCommand.setEnabled(false);
        }
    }
    
    private void requestRootAccess() {
        buttonRequestRoot.setEnabled(false);
        textViewOutput.setText("Requesting root access...");
        
        SMTShellAPI.requestRootAccess(this, new SMTShellAPI.RootAccessCallback() {
            @Override
            public void onResult(boolean granted) {
                buttonRequestRoot.setEnabled(true);
                if (granted) {
                    textViewOutput.setText("Root access granted!");
                    buttonExecuteCommand.setEnabled(true);
                    Toast.makeText(MainActivity.this, "Root access granted", Toast.LENGTH_SHORT).show();
                } else {
                    textViewOutput.setText("Root access denied!");
                    buttonExecuteCommand.setEnabled(false);
                    Toast.makeText(MainActivity.this, "Root access denied", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void executeTestCommand() {
        textViewOutput.setText("Executing test command...");
        
        SMTShellAPI.executeCommand(this, "id", new SMTShellAPI.CommandCallback() {
            @Override
            public void onComplete(String stdout, String stderr, int exitCode) {
                String result = "Command executed!\n\n";
                result += "Exit Code: " + exitCode + "\n";
                result += "Stdout: " + stdout + "\n";
                if (!stderr.isEmpty()) {
                    result += "Stderr: " + stderr + "\n";
                }
                textViewOutput.setText(result);
            }
        });
    }
}
package net.blufenix.smtshell.api;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SMTShellAPI {

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Legacy constants kept for compatibility
    public static final String ACTION_SHELL_COMMAND = "smtshell.intent.action.SHELL_COMMAND";
    public static final String EXTRA_COMMAND = "smtshell.intent.extra.COMMAND";
    public static final String EXTRA_REQUEST_ID = "smtshell.intent.extra.REQUEST_ID";
    public static final String EXTRA_CALLBACK_PKG = "smtshell.intent.extra.CALLBACK_PKG";
    
    // Removed SMT package dependency - now using direct root access
    @Deprecated
    public static final String PKG_NAME_SMT = "com.samsung.SMT";

    public static final String ACTION_LOAD_LIBRARY = "smtshell.intent.action.ACTION_LOAD_LIBRARY";
    public static final String EXTRA_LIBRARY_PATH = "smtshell.intent.extra.EXTRA_LIBRARY_PATH";

    public static final String ACTION_API_PING = "smtshell.intent.action.API_PING";

    // results
    public static final String ACTION_SHELL_RESULT = "smtshell.intent.action.SHELL_RESULT";
    public static final String EXTRA_STDOUT = "smtshell.intent.extra.STDOUT";
    public static final String EXTRA_STDERR = "smtshell.intent.extra.STDERR";
    public static final String EXTRA_EXIT_CODE = "smtshell.intent.extra.EXIT_CODE";

    public static final String ACTION_LOAD_LIBRARY_RESULT = "smtshell.intent.action.LOAD_LIBRARY_RESULT";
    public static final String EXTRA_LOAD_SUCCESS = "smtshell.intent.extra.LOAD_SUCCESS";

    public static final String ACTION_API_READY = "smtshell.intent.action.API_READY";
    public static final String ACTION_API_DEATH_NOTICE = "smtshell.intent.action.API_DEATH_NOTICE";

    // permissions - now using standard Android root permission
    public static final String PERMISSION_SYSTEM_COMMAND = "android.permission.ACCESS_SUPERUSER";
    public static final String PERMISSION_LOAD_LIBRARY = "android.permission.ACCESS_SUPERUSER";
    public static final String PERMISSION_RECEIVER_GUARD = "android.permission.REBOOT";

    // start at a number a user is unlikely to use for themselves,
    //  in case they manually call the API without the wrapper
    private static final AtomicInteger REQUEST_ID = new AtomicInteger(1000000);
    
    private static Process rootProcess;
    
    /**
     * Checks if root access is available on this device
     * @return true if root access is available, false otherwise
     */
    public static boolean isRootAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("su -c 'id'");
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Requests root access from the user
     * This will prompt the user to grant superuser permissions
     * @param context The application context
     * @param callback Callback to handle the result
     */
    public static void requestRootAccess(Context context, RootAccessCallback callback) {
        executor.execute(() -> {
            try {
                // Try to obtain root access
                Process process = Runtime.getRuntime().exec("su");
                process.getOutputStream().write("id\n".getBytes());
                process.getOutputStream().flush();
                process.getOutputStream().close();
                
                int exitCode = process.waitFor();
                boolean granted = (exitCode == 0);
                
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onResult(granted);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onResult(false);
                    }
                });
            }
        });
    }
    static int nextId() {
        return REQUEST_ID.getAndIncrement();
    }

    public static void executeCommand(Context context, String cmd) {
        executeCommand(context, cmd, null);
    }

    public static void executeCommand(Context context, String cmd, CommandCallback cb) {
        executor.execute(() -> {
            try {
                // Execute command as root
                Process process = Runtime.getRuntime().exec("su -c '" + cmd.replace("'", "'\"'\"'") + "'");
                
                // Read stdout
                BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder stdout = new StringBuilder();
                String line;
                while ((line = stdoutReader.readLine()) != null) {
                    stdout.append(line).append("\n");
                }
                
                // Read stderr
                BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder stderr = new StringBuilder();
                while ((line = stderrReader.readLine()) != null) {
                    stderr.append(line).append("\n");
                }
                
                int exitCode = process.waitFor();
                
                // Clean up trailing newlines
                String stdoutStr = stdout.toString();
                String stderrStr = stderr.toString();
                if (stdoutStr.endsWith("\n")) {
                    stdoutStr = stdoutStr.substring(0, stdoutStr.length() - 1);
                }
                if (stderrStr.endsWith("\n")) {
                    stderrStr = stderrStr.substring(0, stderrStr.length() - 1);
                }
                
                // Call back on main thread
                final String finalStdout = stdoutStr;
                final String finalStderr = stderrStr;
                final int finalExitCode = exitCode;
                
                mainHandler.post(() -> {
                    if (cb != null) {
                        cb.onComplete(finalStdout, finalStderr, finalExitCode);
                    }
                });
                
            } catch (IOException | InterruptedException e) {
                mainHandler.post(() -> {
                    if (cb != null) {
                        cb.onComplete("", "Error executing command: " + e.getMessage(), -1);
                    }
                });
            }
        });
    }

    public static void loadLibrary(Context context, String path) {
        loadLibrary(context, path, null);
    }

    public static void loadLibrary(Context context, String path, LoadLibraryCallback cb) {
        executor.execute(() -> {
            try {
                // Load library using root access
                String command = "export LD_LIBRARY_PATH=" + path + ":$LD_LIBRARY_PATH && echo 'Library loaded'";
                Process process = Runtime.getRuntime().exec("su -c '" + command.replace("'", "'\"'\"'") + "'");
                
                int exitCode = process.waitFor();
                boolean success = (exitCode == 0);
                
                mainHandler.post(() -> {
                    if (cb != null) {
                        cb.onComplete(success);
                    }
                });
                
            } catch (IOException | InterruptedException e) {
                mainHandler.post(() -> {
                    if (cb != null) {
                        cb.onComplete(false);
                    }
                });
            }
        });
    }

    public static void ping(Context context) {
        // For compatibility, we'll just broadcast that API is ready
        Intent intent = new Intent(ACTION_API_READY);
        context.sendBroadcast(intent);
    }

    @Deprecated
    static Intent createIntent(String action, int requestId) {
        Intent intent = new Intent(action);
        intent.putExtra(EXTRA_REQUEST_ID, requestId);
        return intent;
    }

    @Deprecated
    static void setSender(Context context, Intent intent) {
        int flags = PendingIntent.FLAG_IMMUTABLE;
        PendingIntent self = PendingIntent.getBroadcast(context, 0, new Intent(), flags);
        intent.putExtra(EXTRA_CALLBACK_PKG, self.getIntentSender());
    }

    public interface CommandCallback {
        void onComplete(String stdout, String stderr, int exitCode);
    }

    public interface LoadLibraryCallback {
        void onComplete(boolean success);
    }
    
    public interface RootAccessCallback {
        void onResult(boolean granted);
    }

}


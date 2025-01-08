package com.mbc.jaspercompiler.models;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class JasperCompilerAPI {
    private final String COMPILE_REPORT_LOG_FILE_PATH = "C:\\ProgramData\\JasperReportCompileLogs.txt";
    private final String COMPILE_ERROR_LOG_FILE_PATH = "C:\\ProgramData\\JasperReportCompileErrors.txt";
    private final String COMPILE_FAIL_LIST_LOG_FILE_PATH = "C:\\ProgramData\\JasperReportCompileFailList.txt";
    private int totalFilesToCompile = 0;
    private int currentCompileFilesCount = 0;
    private int successCompileFilesCount = 0;
    private int failCompileFilesCount = 0;
    private String currentJrxmlCompileFilename = null;
    private String fontName = "mbc";
    private final String jrxmlFilesDirectory;
    private final Label compilingLabel;
    private final Label currentCompiledLabel;
    private final Label totalCompileIndexLabel;
    private final Label compileFilenameLabel;
    private final Label totalCompileLabel;
    private final Label successCompileLabel;
    private final Label failCompileLabel;
    private final ProgressBar progressBar;
    private final Label percentLabel;
    private final Button cancelButton;
    private boolean highPerformanceMode;
    private boolean recursiveMode;
    private final ObservableList<String> recursiveJrxmlFilenameList;

    public JasperCompilerAPI(String jrxmlFilesDirectory, Label compilingLabel,
                             Label currentCompiledLabel, Label totalCompileIndexLabel, Label compileFilenameLabel, Label totalCompileLabel,
                             Label successCompileLabel, Label failCompileLabel, ProgressBar progressBar, Label percentLabel, Button cancelButton) {
        this.jrxmlFilesDirectory = jrxmlFilesDirectory;
        this.compilingLabel = compilingLabel;
        this.currentCompiledLabel = currentCompiledLabel;
        this.totalCompileIndexLabel = totalCompileIndexLabel;
        this.compileFilenameLabel = compileFilenameLabel;
        this.totalCompileLabel = totalCompileLabel;
        this.successCompileLabel = successCompileLabel;
        this.failCompileLabel = failCompileLabel;
        this.progressBar = progressBar;
        this.percentLabel = percentLabel;
        this.cancelButton = cancelButton;
        recursiveJrxmlFilenameList = FXCollections.observableArrayList();
    }
    public void setRecursiveMode(boolean recursiveMode) {
        this.recursiveMode = recursiveMode;
    }

    public void setHighPerformanceMode(boolean set) {
        highPerformanceMode = set;
    }

    public void compileAndExportReport() {
        File jrxmlTempChangeDirectory = new File(jrxmlFilesDirectory + "\\jrxml_temp_change");
        jrxmlTempChangeDirectory.mkdir();
        initialize();
        makeChangesToSourceFile(new File(jrxmlFilesDirectory));
        compileReport();
        Platform.runLater(() -> {
            compilingLabel.setText("All reports compiled successfully...");
            cancelButton.setText("FINISHED");
        });
    }

    private void initialize() {
        totalFilesToCompile = 0;
        currentCompileFilesCount = 0;
        successCompileFilesCount = 0;
        failCompileFilesCount = 0;
        try {
            File errorLogFile = new File(COMPILE_ERROR_LOG_FILE_PATH);
            File failListLogFile = new File(COMPILE_FAIL_LIST_LOG_FILE_PATH);
            File compileLogFile = new File(COMPILE_REPORT_LOG_FILE_PATH);
            if (errorLogFile.exists()) errorLogFile.delete();
            if(failListLogFile.exists()) failListLogFile.delete();
            if(compileLogFile.exists()) compileLogFile.delete();
            errorLogFile.createNewFile();
            failListLogFile.createNewFile();
            compileLogFile.createNewFile();
        } catch(IOException ioe) {
            System.out.println("Failed to create or delete log files... - "+ioe.getMessage());
        }
    }

    private void compileReport() {
        startProgressBarThread();
        recursiveJrxmlFilenameList.forEach(this::compileReportAlgorithm);
        Platform.runLater(()->compilingLabel.setText("all reports compiled successfully..."));
        updateUI();
    }
    private void compileReportAlgorithm(String sourceFilePath) {
        currentJrxmlCompileFilename = sourceFilePath;
        String jasperFilePath = sourceFilePath.substring(0, sourceFilePath.lastIndexOf("."))+".jasper";
        updateUI();
        try {
            if (!highPerformanceMode) Thread.sleep(new Random().nextInt(3000));
            JasperCompileManager.compileReportToFile(sourceFilePath, jasperFilePath);
            successCompileFilesCount++;
            logFileExporter(sourceFilePath, COMPILE_REPORT_LOG_FILE_PATH);
        } catch (JRException e) {
            failCompileFilesCount++;
            logFileExporter(sourceFilePath, COMPILE_FAIL_LIST_LOG_FILE_PATH);
            errorLogFileExporter(sourceFilePath, e.getMessage());
        } catch (InterruptedException e) {
            errorLogFileExporter(sourceFilePath, e.getMessage());

        }
        currentCompileFilesCount++;
        System.out.println("Compiled reports successfully...");
    }

    private void makeChangesToSourceFile(File sourceDirectory) {
        if ( sourceDirectory.isDirectory() ) {
            if (highPerformanceMode) {
                Arrays.stream(Objects.requireNonNull(sourceDirectory.listFiles())).parallel().forEach(directory -> {
                    if ( directory.isDirectory() && recursiveMode ) {
                        makeChangesToSourceFile(directory);
                    } else {
                        if (isJrxmlFile(directory.getAbsolutePath())) {
                            recursiveJrxmlFilenameList.add(directory.getAbsolutePath());
                            makeChangesAlgorithm(directory.getAbsolutePath());
                        }
                    }
                });
            } else {
                for ( File directory : Objects.requireNonNull(sourceDirectory.listFiles())) {
                    if ( directory.isDirectory() && recursiveMode ) {
                        makeChangesToSourceFile(directory);
                    } else {
                        if (isJrxmlFile(directory.getAbsolutePath())) {
                            recursiveJrxmlFilenameList.add(directory.getAbsolutePath());
                            makeChangesAlgorithm(directory.getAbsolutePath());
                        }
                    }
                }
            }
        } else {
            if (isJrxmlFile(sourceDirectory.getAbsolutePath())) {
                recursiveJrxmlFilenameList.add(sourceDirectory.getAbsolutePath());
                makeChangesAlgorithm(sourceDirectory.getAbsolutePath());
            }
        }
    }

    private synchronized void makeChangesAlgorithm(String path) {
        try {
            String jrxmlChangedFilepath = path.substring(0, path.lastIndexOf("."))+"_temp_changed.jrxml";
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(path), StandardCharsets.UTF_8));
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(jrxmlChangedFilepath), StandardCharsets.UTF_8));
            String readLine;
            while ( (readLine = bufferedReader.readLine()) != null ) {
                if ( readLine.contains("fontName")) {
                    readLine = readLine.replaceAll("fontName=\"[^\"]*\"", "fontName=\""+fontName+"\"");
                }
                bufferedWriter.write(readLine.concat("\n"));
            }
            bufferedReader.close();
            bufferedWriter.close();
            new File(path).delete();
            new File(jrxmlChangedFilepath).renameTo(new File(jrxmlChangedFilepath.replace("_temp_changed.jrxml", ".jrxml")));
        } catch (IOException e) {
            System.out.println("Error in make changes."+ e.getMessage());
            System.out.println("Filepath: "+path);
            errorLogFileExporter(path, e.getMessage());
            e.printStackTrace();
        }
        totalFilesToCompile++;
        Platform.runLater(()-> totalCompileLabel.setText(String.valueOf(totalFilesToCompile)));
        Platform.runLater(()-> totalCompileIndexLabel.setText(String.valueOf(totalFilesToCompile)));
    }

    private boolean isJrxmlFile(String filePath) {
        try {
            return filePath.substring(filePath.lastIndexOf(".")).equalsIgnoreCase(".jrxml");
        } catch (Exception e) {
            errorLogFileExporter(filePath, e.getMessage());
            return false;
        }
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    public void updateUI() {
        Platform.runLater(() -> {
            compilingLabel.setText("compiling reports...");
            currentCompiledLabel.setText(String.valueOf(currentCompileFilesCount));
            totalCompileIndexLabel.setText(String.valueOf(totalFilesToCompile));
            compileFilenameLabel.setText(currentJrxmlCompileFilename);
            totalCompileLabel.setText(String.valueOf(totalFilesToCompile));
            successCompileLabel.setText(String.valueOf(successCompileFilesCount));
            failCompileLabel.setText(String.valueOf(failCompileFilesCount));
            percentLabel.setText(String.valueOf(getCurrentCompilePercentage()).concat("%"));
            System.out.println(getCurrentCompilePercentage());
        });
    }

    private int getCurrentCompilePercentage() {
        return (int)(((double)currentCompileFilesCount/(double)totalFilesToCompile)*100);
    }

    private void startProgressBarThread() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                while(currentCompileFilesCount <= totalFilesToCompile) {
                    updateProgress(currentCompileFilesCount, totalFilesToCompile);
                }
                return null;
            }
        };
        Platform.runLater(() -> progressBar.progressProperty().bind(task.progressProperty()));

        // Run the task on a background thread
        Thread progressBarThread = new Thread(task);
        progressBarThread.setDaemon(true);  // The thread will terminate when the application ends
        progressBarThread.start();
    }

    private void logFileExporter(String filename, String filePath) {
        try(BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filePath,true))) {
            bufferedWriter.write(filename+"\n");
        } catch (IOException ioe) {
            System.out.println("Error while exporting compile log file "+filePath);
        }
    }
    private void errorLogFileExporter(String filename, String errorMessage) {
        try(BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(COMPILE_ERROR_LOG_FILE_PATH,true))) {
            bufferedWriter.write(filename+"\n");
            bufferedWriter.write(errorMessage);
        } catch (IOException ioe) {
            System.out.println("Error while exporting compile log file "+COMPILE_ERROR_LOG_FILE_PATH);
        }
    }
}
package com.magenta.maxoptra.integration.gp.application;

import com.magenta.maxoptra.integration.gp.configuration.ArchiveConf;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ArchiveService {

    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);
    private static final String DEFAULT_DATE_FORMAT_PATTERN = "yyyy_MM_dd__HH_mm_ss";
    private static final String SHORT_DATE_FORMAT_PATTERN = "HH_mm_ss_SSS";

    public static void archiveExportMessage(ArchiveConf archiveConf, String message) {
        log.info("Start archive export message");
        if (archiveConf == null || StringUtils.isBlank(archiveConf.exportFolder)) {
            log.info("Archive export folder not set");
            return;
        }

        String pattern = archiveConf.archivePattern;
        SimpleDateFormat sdf = new SimpleDateFormat(StringUtils.isBlank(pattern) ? DEFAULT_DATE_FORMAT_PATTERN : SHORT_DATE_FORMAT_PATTERN);
        String exportFolder = archiveConf.exportFolder;

        String archiveExportFolderPath = createArchivePath(exportFolder, pattern);
        if (pattern != null) {
            createPatternArchiveFolder(archiveExportFolderPath);
        }
        String fileName = archiveExportFolderPath + sdf.format(new Date());
        archiveMessage(message, fileName);
    }

    public static void archiveImportMessage(ArchiveConf archiveConf, String message) {
        log.info("Start archive import message");
        if (archiveConf == null || StringUtils.isBlank(archiveConf.importFolder)) {
            log.info("Archive import folder not set");
            return;
        }
        String pattern = archiveConf.archivePattern;
        SimpleDateFormat sdf = new SimpleDateFormat(StringUtils.isBlank(pattern) ? DEFAULT_DATE_FORMAT_PATTERN : SHORT_DATE_FORMAT_PATTERN);
        String exportFolder = archiveConf.importFolder;

        String archiveExportFolderPath = createArchivePath(exportFolder, pattern);
        if (pattern != null) {
            createPatternArchiveFolder(archiveExportFolderPath);
        }
        String fileName = archiveExportFolderPath + sdf.format(new Date());
        archiveMessage(message, fileName);
    }

    public static void archiveErrorMessage(ArchiveConf archiveConf, String message, Exception ex) {
        log.info("Start archive error message");

        message += "\n\n----Error message----\n\n";
        message += ex.getMessage() != null ? ex.getMessage() + "/n" : "" + ex.toString();

        if (archiveConf == null || StringUtils.isBlank(archiveConf.exportErrorFolder)) {
            log.info("Archive export folder not set");
            return;
        }
        String exportFolder = archiveConf.exportErrorFolder;

        String archiveExportFolderPath = createArchivePath(exportFolder, null);
        SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_DATE_FORMAT_PATTERN);
        String fileName = archiveExportFolderPath + sdf.format(new Date());
        archiveMessage(message, fileName);
    }

    public static void archiveMessage(@NotNull String message, @NotNull String path) {
        try {
            File file = new File(path);
            FileUtils.writeStringToFile(file, message);
            log.info("Message archived in file - " + path);
        } catch (Exception ex) {
            log.error("Error when try to archive message:", ex);
        }
    }

    private static String createArchivePath(@NotNull String path, String pattern) {
        String last = path.trim().substring(path.length() - 1);
        if (!last.equals("/") && !last.equals("\\")) {
            path += File.separator;
        }
        if (pattern != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern.replaceAll("[/\\\\]", File.separator));
            path += sdf.format(new Date());
            path = createArchivePath(path, null);
        }
        return path;
    }

    private static void createPatternArchiveFolder(String path) {
        File patternFolder = new File(path);
        if (!patternFolder.exists()) {
            patternFolder.mkdir();
        }
    }
}

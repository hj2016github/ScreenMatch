package com.duke.screenmatch.utils;

import com.duke.screenmatch.settings.Settings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlTag;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class Utils {
    public static int DIALOG_WIDTH = 400;
    public static int DIALOG_HEIGHT = 450;
    private static String[] ignoreFoldersDefault = {".gradle", "gradle", ".idea", "build", ".git"};

    //PropertiesComponent.getInstance().setValue() //保存基本类型及String等
    //PropertiesComponent.getInstance().setValues() //可保存数字

    public static Point getDialogCenterLocation() {
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        Point point = new Point();
        point.x = (dimension.width - DIALOG_WIDTH) >> 1;
        point.y = (dimension.height - DIALOG_HEIGHT) >> 1;
        return point;
    }

    public static boolean isEmpty(String string) {
        if (string == null || "".equals(string.trim())) {
            return true;
        }
        return false;
    }

    public static ArrayList<String> getModuleNames(String basePath) {
        if (isEmpty(basePath)) {
            return null;
        }
        ArrayList<String> names = new ArrayList<>();
        VirtualFile baseVirtualFile = getVirtualFile(basePath);
        if (baseVirtualFile == null) {
            return null;
        }
        VirtualFile[] virtualFiles = baseVirtualFile.getChildren();
        if (virtualFiles == null || virtualFiles.length <= 0) {
            return null;
        }
        VirtualFile temp = null;
        String markUsuallyUsed = null;
        String defaultUsed = Settings.getDefaultModuleName();
        for (int i = 0; i < virtualFiles.length; i++) {
            temp = virtualFiles[i];
            if (temp == null
                    || !temp.isValid()
                    || !temp.isDirectory()) {
                continue;
            }
            if (isIgnore(basePath, temp.getName())) {
                continue;
            }
            if (!Utils.isEmpty(defaultUsed) && defaultUsed.equals(temp.getName())) {
                markUsuallyUsed = temp.getName();
                continue;
            }
            names.add(temp.getName());
        }
        if (!Utils.isEmpty(markUsuallyUsed)) {
            names.add(0, markUsuallyUsed);
        }
        return names;
    }

    private static boolean isIgnore(String basePath, String targetValue) {
        if (isEmpty(targetValue)) {
            return true;
        }
        Set<String> set = new HashSet<>();
        set.addAll(Arrays.asList(ignoreFoldersDefault));
        String ignoreModule = Settings.get(basePath, Settings.KEY_IGNORE_MODULE_NAME);
        if (!isEmpty(ignoreModule)) {
            ignoreModule = ignoreModule.replaceAll("，", ",")
                    .replaceAll("， ", ",")
                    .replaceAll(" ，", ",")
                    .replaceAll(" ， ", ",")
                    .replaceAll(", ", "")
                    .replaceAll(" ,", "")
                    .replaceAll(" , ", "");
        }
        if (!isEmpty(ignoreModule)) {
            String[] readModules = ignoreModule.split(",");
            set.addAll(Arrays.asList(readModules));
        }

        Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            String item = iterator.next();
            if (isEmpty(item)) {
                continue;
            }
            if (targetValue.equals(item.trim())) {
                return true;
            }
        }
        return false;
    }

    private void setSelectText(AnActionEvent event, String text) {
        if (event == null || Utils.isEmpty(text)) {
            return;
        }
        Project project = event.getData(PlatformDataKeys.PROJECT);
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        Document document = editor.getDocument();
        SelectionModel selectionModel = editor.getSelectionModel();
        final int start = selectionModel.getSelectionStart();
        final int end = selectionModel.getSelectionEnd();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                document.replaceString(start, end, text);
            }
        };
        WriteCommandAction.runWriteCommandAction(project, runnable);
        selectionModel.removeSelection();
    }

    /**
     * 检查或创建子目录
     *
     * @param basePath 父目录路径
     * @param childDir 当前需要创建的目录
     * @return 目录是否存在或创建
     */
    public boolean checkAndCreateChildDir(String basePath, String childDir) {
        if (isEmpty(basePath) || isEmpty(childDir)) {
            return false;
        }
        VirtualFile virtualFile = getVirtualFile(basePath);
        if (virtualFile == null) {
            return false;
        }
        // 判断根目录下是否有childDir文件夹
        VirtualFile dbDir = virtualFile.findChild(childDir);
        if (dbDir == null) {
            // 没有就创建一个
            try {
                virtualFile = virtualFile.createChildDirectory(null, childDir);
                if (virtualFile.isDirectory()
                        && virtualFile.isWritable() && virtualFile.isValid()) {
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static VirtualFile getVirtualFile(String path) {
        if (isEmpty(path)) {
            return null;
        }
        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    public static PsiFile getPsiFile(VirtualFile virtualFile, Project project) {
        if (virtualFile == null || project == null) {
            return null;
        }
        return PsiManager.getInstance(project).findFile(virtualFile);
    }

    public static String getBasePath(Project project) {
        if (project == null) {
            return null;
        }
        return project.getBasePath() + File.separator;
    }

    /**
     * 获取res目录，包含 layout, values, drawable 或 mipmap
     *
     * @param basePath
     * @return res路径 <br/>
     * 1、module/AndroidManifest.xml <br/>
     * module/res/values <br/>
     * <p>
     * 2、module/src/main/AndroidManifest.xml <br/>
     * module/src/main/res/values <br/>
     */
    public static String getResPath(String basePath, String moduleName) {
        if (isEmpty(basePath) || isEmpty(moduleName)) {
            return null;
        }
        String returnPath = "";
        String targetPath = basePath + File.separator + moduleName + File.separator + "res" + File.separator;
        VirtualFile virtualFile = getVirtualFile(targetPath);
        if (virtualFile == null || !virtualFile.isValid() || !virtualFile.isDirectory()) {
            targetPath = basePath + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "res" + File.separator;
            virtualFile = getVirtualFile(targetPath);
            if (virtualFile == null || !virtualFile.isValid() || !virtualFile.isDirectory()) {
                //returnPath = "";
            } else {
                returnPath = targetPath;
            }
        } else {
            returnPath = targetPath;
        }
        return returnPath;
    }

    public static String getAndroidManifestPath(String basePath, String moduleName) {
        if (isEmpty(basePath) || isEmpty(moduleName)) {
            return null;
        }
        String returnPath = "";
        String targetPath = basePath + File.separator + moduleName + File.separator + "AndroidManifest.xml";
        VirtualFile virtualFile = getVirtualFile(targetPath);
        if (virtualFile == null || !virtualFile.isValid() || !virtualFile.isDirectory()) {
            targetPath = basePath + File.separator + moduleName + File.separator + "src" + File.separator + "main" + File.separator + "AndroidManifest.xml";
            virtualFile = getVirtualFile(targetPath);
            if (virtualFile == null || !virtualFile.isValid() || !virtualFile.isDirectory()) {
                //returnPath = "";
            } else {
                returnPath = targetPath;
            }
        } else {
            returnPath = targetPath;
        }
        return returnPath;
    }

    //获取包名字符串
    public static String getAppPackageName(Project project, String moduleName) {
        if (project == null || isEmpty(moduleName)) {
            return null;
        }
        PsiFile manifestFile = null;
        try {
            manifestFile = getPsiFile(getVirtualFile(getAndroidManifestPath(getBasePath(project), moduleName)), project);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (manifestFile == null) {
            return null;
        }
        XmlDocument xml = (XmlDocument) manifestFile.getFirstChild();
        if (xml == null) {
            return null;
        }
        XmlTag xmlTag = xml.getRootTag();
        if (xmlTag == null) {
            return null;
        }
        XmlAttribute xmlAttribute = xmlTag.getAttribute("package");
        if (xmlAttribute == null) {
            return null;
        }
        return xmlAttribute.getValue();
    }
}
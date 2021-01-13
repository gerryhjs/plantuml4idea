package org.plantuml.idea.grammar;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.java.stubs.index.JavaShortClassNameIndex;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.plantuml.idea.grammar.psi.PumlItem;
import org.plantuml.idea.lang.PlantUmlFileType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PumlPsiUtil {

    public static List<PumlItem> findDeclarationOrUsagesInFile(PsiFile containingFile, @NotNull PumlItem element, String key) {
        List<PumlItem> result = new ArrayList<>();
        PumlItem[] items = PsiTreeUtil.getChildrenOfType(containingFile, PumlItem.class);
        boolean returnFirst = true;
        boolean firstMatch = true;

        if (items != null) {
            for (PumlItem item : items) {
                if (isSame(key, item.getText())) {
                    if (firstMatch && item == element) {
                        returnFirst = false;
                        continue;
                    }

                    result.add(item);
                    if (returnFirst) {
                        break;
                    }
                    firstMatch = false;
                }
            }
        }
        return result;
    }

    public static PumlItem findDeclarationInFile(PsiFile containingFile, @NotNull PumlItem element, String key) {
        PumlItem[] items = PsiTreeUtil.getChildrenOfType(containingFile, PumlItem.class);
        if (items != null) {
            for (PumlItem item : items) {
                if (item == element) {
                    return null;
                }
                if (isSame(key, item.getText())) {
                    return item;
                }
            }
        }
        return null;
    }

    static final Pattern SANITIZER = Pattern.compile("([a-zA-Z0-9].*[a-zA-Z0-9])");

    public static boolean isSame(String p1, String p2) {
        if (p1.equals(p2)) {
            return true;
        }
        Matcher m = SANITIZER.matcher(p1);
        String key = null;
        if (m.find()) {
            key = m.group();
        }
        if (key == null) {
            return false;
        }

        String key2 = null;
        m.reset(p2);
        if (m.find()) {
            key2 = m.group();
        }
        return key.equals(key2);
    }

    public static List<PumlItem> findDeclarationInAllFiles(Project project, String key) {
        List<PumlItem> result = new ArrayList<>();
        Collection<VirtualFile> virtualFiles = FileTypeIndex.getFiles(PlantUmlFileType.INSTANCE, GlobalSearchScope.allScope(project));
        for (VirtualFile virtualFile : virtualFiles) {
            PsiFile simpleFile = (PsiFile) PsiManager.getInstance(project).findFile(virtualFile);
            if (simpleFile != null) {
                PumlItem[] properties = PsiTreeUtil.getChildrenOfType(simpleFile, PumlItem.class);
                if (properties != null) {
                    for (PumlItem property : properties) {
                        if (isSame(key, property.getText())) {
                            result.add(property);
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    public static List<PumlItem> findAll(PsiFile file) {
        List<PumlItem> result = new ArrayList<>();
        if (file != null) {
            PumlItem[] items = PsiTreeUtil.getChildrenOfType(file, PumlItem.class);
            if (items != null) {
                Collections.addAll(result, items);
            }
        }
        return result;
    }

    public static Collection<PsiClass> findJavaClass(PumlItem element) {
        if (DumbService.isDumb(element.getProject())) {
            return Collections.emptyList();
        }
        String text = element.getText();

        Module moduleForFile = ModuleUtilCore.findModuleForFile(element.getContainingFile());
        if (moduleForFile != null) {
            GlobalSearchScope scope = GlobalSearchScope.moduleScope(moduleForFile);
            return JavaShortClassNameIndex.getInstance().get(text, element.getProject(), scope);
        } else {
            return JavaShortClassNameIndex.getInstance().get(text, element.getProject(), GlobalSearchScope.allScope(element.getProject()));
        }
    }
}
/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.plugins.groovy.annotator.intentions;

import com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFixBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.*;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.GrReferenceElement;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter;
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.imports.GrImportStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrCodeReferenceElement;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrTypeElement;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

import java.util.Collection;
import java.util.List;

/**
 * @author peter
 */
public class GroovyAddImportAction extends ImportClassFixBase<GrReferenceElement<?>, GrReferenceElement<?>> {
  private final GrReferenceElement<?> ref;

  public GroovyAddImportAction(@NotNull GrReferenceElement<?> ref) {
    super(ref, ref);
    this.ref = ref;
  }

  @Override
  protected String getReferenceName(@NotNull GrReferenceElement<?> reference) {
    return reference.getReferenceName();
  }

  @Override
  protected PsiElement getReferenceNameElement(@NotNull GrReferenceElement<?> reference) {
    return reference.getReferenceNameElement();
  }

  @Override
  protected boolean hasTypeParameters(@NotNull GrReferenceElement<?> reference) {
    return reference.getTypeArguments().length > 0;
  }

  @Override
  protected String getQualifiedName(@NotNull GrReferenceElement<?> referenceElement) {
    return referenceElement.getCanonicalText();
  }

  @Override
  protected boolean isQualified(@NotNull GrReferenceElement<?> reference) {
    return reference.getQualifier() != null;
  }

  @Override
  protected boolean hasUnresolvedImportWhichCanImport(PsiFile psiFile, String name) {
    if (!(psiFile instanceof GroovyFile)) return false;
    final GrImportStatement[] importStatements = ((GroovyFile)psiFile).getImportStatements();
    for (GrImportStatement importStatement : importStatements) {
      final GrCodeReferenceElement importReference = importStatement.getImportReference();
      if (importReference == null || importReference.resolve() != null) {
        continue;
      }
      if (importStatement.isOnDemand() || Comparing.strEqual(importStatement.getImportedName(), name)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected @NotNull Collection<PsiClass> filterByContext(@NotNull Collection<PsiClass> candidates, @NotNull GrReferenceElement<?> referenceElement) {
    PsiElement typeElement = referenceElement.getParent();
    if (typeElement instanceof GrTypeElement) {
      PsiElement decl = typeElement.getParent();
      if (decl instanceof GrVariableDeclaration) {
        GrVariable[] vars = ((GrVariableDeclaration)decl).getVariables();
        if (vars.length == 1) {
          PsiExpression initializer = vars[0].getInitializer();
          if (initializer != null) {
            PsiType type = initializer.getType();
            if (type != null) {
              return filterAssignableFrom(type, candidates);
            }
          }
        }
      }
      if (decl instanceof GrParameter) {
        return filterBySuperMethods((PsiParameter)decl, candidates);
      }
    }

    return super.filterByContext(candidates, referenceElement);
  }

  @Override
  protected String getRequiredMemberName(@NotNull GrReferenceElement<?> referenceElement) {
    if (referenceElement.getParent() instanceof GrReferenceElement) {
      return ((GrReferenceElement<?>)referenceElement.getParent()).getReferenceName();
    }
    return super.getRequiredMemberName(referenceElement);
  }

  @Override
  protected boolean isAccessible(@NotNull PsiMember member, @NotNull GrReferenceElement<?> referenceElement) {
    return true;
  }

  @Override
  public boolean showHint(@NotNull Editor editor) {
    // Lines below are required to prevent the "Add import" popup appearing two times in a row
    // Similar issue is in com.intellij.codeInsight.daemon.impl.quickfix.ImportClassFixBase.calcClassesToImport
    if (!ref.isValid()) {
      return false;
    }
    PsiFile containingFile = ref.getContainingFile();
    if (containingFile instanceof GroovyFile) {
      List<PsiClass> alreadyImportedClasses =
        ContainerUtil.map(((GroovyFile)containingFile).getImportStatements(), GrImportStatement::resolveTargetClass);
      for (PsiClass classToImport : getClassesToImport()) {
        if (alreadyImportedClasses.contains(classToImport)) {
          return false;
        }
      }
    }
    return super.showHint(editor);
  }

  @Override
  protected void bindReference(@NotNull PsiReference reference, @NotNull PsiClass targetClass) {
    PsiElement referringElement = reference.getElement();
    if (referringElement.getParent() instanceof GrMethodCall &&
        referringElement instanceof GrReferenceExpression &&
        PsiUtil.isNewified(referringElement)) {
      handleNewifiedClass(referringElement, targetClass);
    }
    else {
      super.bindReference(reference, targetClass);
    }
  }

  private static void handleNewifiedClass(@NotNull PsiElement referringElement, @NotNull PsiClass targetClass) {
    PsiFile file = referringElement.getContainingFile();
    if (file instanceof GroovyFile) {
      ((GroovyFile)file).importClass(targetClass);
    }
  }
}

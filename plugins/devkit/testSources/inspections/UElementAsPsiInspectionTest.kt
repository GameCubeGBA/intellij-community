/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package org.jetbrains.idea.devkit.inspections

class UElementAsPsiInspectionTest : PluginModuleTestCase() {

  override fun setUp() {
    super.setUp()
    myFixture.addClass("package org.jetbrains.uast; public interface UElement {}")
    myFixture.addClass("""package com.intellij.psi; public interface PsiElement {
      | PsiElement getParent();
      |}""".trimMargin())
    myFixture.addClass("package com.intellij.psi; public interface PsiClass extends PsiElement {}")
    myFixture.addClass("""package org.jetbrains.uast; public interface UClass extends UElement, com.intellij.psi.PsiClass {
      | void uClassMethod();
      |}""".trimMargin())

    myFixture.enableInspections(UElementAsPsiInspection())
  }

  fun testPassAsPsiClass() {
    //language=JAVA
    myFixture.addClass("""
      import org.jetbrains.uast.UClass;
      import com.intellij.psi.PsiClass;

      class UastUsage {

          public void processUClassCaller(UClass uClass){
             processUClass(uClass);
          }

          public void processUClass(UClass uClass){ processPsiClass(<warning descr="Usage of UElement as PsiElement is not recommended">uClass</warning>); }

          public void processPsiClass(PsiClass psiClass){ }

      }
    """.trimIndent())
    myFixture.testHighlighting("UastUsage.java")

  }

  fun testAssignment() {
    //language=JAVA
    myFixture.addClass("""
      import org.jetbrains.uast.UClass;
      import com.intellij.psi.PsiClass;

      class UastUsage {

          PsiClass psiClass;

          public UastUsage(UClass uClass){
             psiClass = <warning descr="Usage of UElement as PsiElement is not recommended">uClass</warning>;
             PsiClass localPsiClass = <warning descr="Usage of UElement as PsiElement is not recommended">uClass</warning>;
          }

      }
    """.trimIndent())
    myFixture.testHighlighting("UastUsage.java")

  }

  fun testCallParentMethod() {
    //language=JAVA
    myFixture.addClass("""
      import org.jetbrains.uast.UClass;
      import com.intellij.psi.PsiClass;

      class UastUsage {

          public UastUsage(UClass uClass){
             <warning descr="Usage of UElement as PsiElement is not recommended">uClass.getParent()</warning>;
             uClass.uClassMethod();
          }

      }
    """.trimIndent())
    myFixture.testHighlighting("UastUsage.java")

  }

  fun testImplementAndCallParentMethod() {
    //language=JAVA
    myFixture.addClass("""
      import org.jetbrains.uast.UClass;
      import com.intellij.psi.*;

      public class UastUsage {

          public UastUsage(){
            UClassImpl impl = new UClassImpl();
            <warning descr="Usage of UElement as PsiElement is not recommended">impl.getParent()</warning>;
            impl.uClassMethod();
          }

      }

      class UClassImpl implements UClass {
        public PsiElement getParent(){ return null; }
        public void uClassMethod(){  }
      }

    """.trimIndent())
    myFixture.testHighlighting("UastUsage.java")

  }


}
# Package Info Maven Plugin

## Contents

* [What is it?](#what-is-it)
* [Prerequisites](#prerequisites)
* [Installation](#installation)
* [General Usage](#general-usage)
* [Sample Project Layout](#sample-project-layout)
* [Basic Configuration](#basic-config)
* [Multiple Templates](#multiple-templates)
* [Configuration Options](#config-options)


## <a name="what-is-it"></a>What is it?

The Package Info Maven Plugin generates package-info.java files based on a template file.

The main motivation for this plugin was to apply JSR305 Nullability annotations project wide in order to make libraries
more Kotlin-friendly

## <a name="prerequisites"></a>Requirements

- Java 8+


## <a name="installation"></a>Installation

```xml
<build>
    <plugins>
        <plugin>
            <groupId>com.github.bohnman</groupId>
            <artifactId>package-info-maven-plugin</artifactId>
            <version>1.0.1</version>
           <configuration>
               <packages>
                   <package>
                       <pattern>**</pattern>
                       <template>${project.basedir}/src/main/java/your/base/package/package-info.java</template>
                   </package>
               </packages>
           </configuration>
           <executions>
               <execution>
                   <goals>
                       <goal>generate</goal>
                   </goals>
               </execution>
           </executions>
        </plugin>
    </plugins>
</build>
```

## <a name="general-usage"></a>General Usage

```
mvn package-info:generate
```

```
mvn package-info:help
```

Also execute at the generate-sources phase by default if you have an execution configured.

## <a name="sample-project-layout"></a>Sample Project Layout

Let's use the following project layout

```
src/main/java/com/github/bohnman/example
|-- package1
|   |-- Foo.java
|-- package2
|   |-- package-info.java
|   |-- Bar.java
|-- package3
|   |-- package-info.java
|   |-- Baz.java 
|-- package4
|-- package5
    |-- Bear.java
|-- template1
    |-- package-info.java
|-- template2
    |-- package-info.java       
```

Contents of template1/package-info.java:

```java
// Template 1
package com.github.bohnman.example.template1;
```

Contents of template2/package-info.java:

```java
// Template 2
package com.github.bohnman.example.template2;
```

Contents of package2/package-info.java:

```java
// Package2
package com.github.bohnman.example.package2;
```

Contents of package3/package-info.java:
```java
// Package3 Before
// <replace>
package com.github.bohnman.example.package2;
// </replace>
// Package3 After
```

## <a name="basic-config"></a>Basic Configuration

Based on project layout, let's look at a basic configuration for the plugin

```xml
<configuration>
  <packages>
    <package>
        <pattern>**</pattern>
        <template>${project.basedir}/src/main/java/com/github/bohnman/example/template1/package-info.java</template>        
    </package>
  </packages>
</configuration>
```


Now let's run the following on the command line:
```
mvn package-info:generate
```

Once this is run, you'll make several observations:

1)  Two new files are created:

        - generated/src/main/java/com/github/bohnman/example/package1/package-info.java
        - generated/src/main/java/com/github/bohnman/example/package5/package-info.java

    Contents of package1/package-info.java:
    
    ```java
    // Template 1
    package com.github.bohnman.example.package1;
    ```
    
    Contents of package5/package-info.java:        
    ```java
    // Template 1
    package com.github.bohnman.example.package5;
    ```
        
    Both files we're generated with the correct template and the correct package path    

2)  There was no generated package-info file for package2.  This is because a package-info.java file already exists in
    the main source.
    
3)  There was no generate package-info file generated for package4.  This is because there are no java files in that 
    directory.
    
4)  The contents of ``src/main/java/com/github/bohnman/example/package3/package-info.java`` were changed to:

    ```java
    // Package3 Before
    // <replace>
    // Template 1
    package com.github.bohnman.example.package3;
    // </replace>
    // Package3 After
    ```
    
    This is because the package-info.java contains special <replace></replace> markers, which tells the plugin to 
    replace the source file inline.
    
## <a name="multiple-templates"></a>Multiple Templates

Let's use the following configuration

```xml
<configuration>
  <packages>
    <package>
      <pattern>**.package5</pattern>
      <template>${project.basedir}/src/main/java/com/github/bohnman/example/template2/package-info.java</template>        
    </package>
    <package>
        <pattern>**</pattern>
        <template>${project.basedir}/src/main/java/com/github/bohnman/example/template1/package-info.java</template>        
    </package>
  </packages>
</configuration>
```


Now, the generated package-info.java for package5 will use template2 instead of template1.


A couple of things to note:
- patterns are matching against the package name (i.e. foo.bar) and NOT the directory path.
- patterns use Ant-style matching syntax (although regexes can be used).  See [here](https://raw.githubusercontent.com/sonatype/plexus-utils/master/src/main/java/org/codehaus/plexus/util/SelectorUtils.java) for more information
- The first package declaration to match the package wins, so you'll want to put your more specific patterns first. 

## <a name="multiple-templates"></a>Kotlin Example

As stated previously, the main motivation for this project was to provide the ability to help with nullability 
JSR 305 annotations for library creators to provide better Java interoperability with Kotlin.

A current limitation with this support is that nullability cannot be defined at a project level.  Instead, the least
granular choice is at a package level.  While this provides some help (as opposed to putting annotations on every 
class, method, etc.), it is still cumbersome for libraries with several packages.  This plugin alleviates this burden, 
by automatically generating package-info.java files with any annotations that are desired.


Here is an example of how we would provide nullability annotations for our whole project.

First, we declare JSR305 as a dependency:

```xml
<dependency>
    <groupId>com.google.code.findbugs</groupId>
    <artifactId>jsr305</artifactId>
    <version>3.0.2</version>
</dependency>
```

Next, we create a NullableApi annotation that uses our JSR305 annotations;

```java
package com.github.bohnman.example;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import javax.annotation.meta.When;
import java.lang.annotation.ElementType;

@Nonnull(when = When.MAYBE)
@TypeQualifierDefault({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE_USE})
public @interface NullableApi {
}
``` 

Then, we create a template package-info.java

```java
@NullableApi
package com.github.bohnman.example;
```

Then, we configure the package import plugin to use the template package

```xml
<configuration>
  <packages>
    <package>
        <pattern>**</pattern>
        <template>${project.basedir}/src/main/java/com/github/bohnman/example/package-info.java</template>        
    </package>
  </packages>
</configuration>
```

Then, we create an example class

```java 

package com.github.bohnman.example;

import javax.annotation.Nonnull;

public class Util {
   
   public static Date getDate1() {
       return new Date();
   }
   
   @Nonnull
   public static Date getDate2() {
       return new Date();
   }
}

```

Finally, we package our library up and use it in our kotlin project

Note: you need to provide the kotlin compiler with the argument: ``-Xjsr305=strict``

```kotlin
package com.github.bohnman.kotlin

import com.github.bohnman.example.Util

fun main(args: Array<String>) {
   println(Util.getDate1().getTime()) // error, because of NullableApi annotation, the type returned is actually Date?
   println(Util.getDate1()?.getTime()) // works because we use the safe navigation operator 
   println(Util.getDate2().getTime()) // works because we annotated the getDate2 method with @Nonnull
}
```


For more information Kotlin and Java and null safety, [see here](https://kotlinlang.org/docs/reference/java-interop.html). 


## <a name="config-options"></a>Configuration Options

| Name                 | Description   |
| -------------        | ------------- |
| generate             | Perform generation of new package-info.java files? (Default: ``true``)  |
| inline               | Perform inline replacement of existing package-info.java (Default: ``true``)  |
| inlineReplacePattern | Pattern to use for inline replacement (Default: ``(\s*//\s*<replace>\s*)(.*?)(\s*//\s*</replace>\s*)``)  |
| outputDirectory      | Directory to place generated package-info.java files (Default: ``${project.basedir}/generated/src/main/java``)  |
| packages             | Package pattern and template configuration (Required)  |
| sourceDirectory      | Main source directory (Default: ``${project.basedir}/src/main/java``)  |


### Individual Package Configuration

| Name                 | Description   |
| -------------        | ------------- |
| caseSensitive        | Indicates whether the pattern matching is case sensitive (Default: ``true``)
| pattern              | Pattern to match the current package name against (Required)
| template             | Path to template file (Required)

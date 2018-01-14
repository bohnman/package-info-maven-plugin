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
            <version>1.0-SNAPSHOT</version>
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
        - ``generated/src/main/java/com/github/bohnman/example/package1/package-info.java``
        - ``generated/src/main/java/com/github/bohnman/example/package5/package-info.java``

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
- pattern using Ant-style matching syntax (although regexes can be used).  See [here](https://raw.githubusercontent.com/sonatype/plexus-utils/master/src/main/java/org/codehaus/plexus/util/SelectorUtils.java) for more information
- The first package declaration to match the package wins, so you'll want to put your more specific patterns first. 



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

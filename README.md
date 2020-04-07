# Guyver Object Modifier
--------------
[![Maven Central](https://img.shields.io/maven-central/v/io.github.chapeco.api.testrail/testrail-api-java-client.svg?label=Maven%20Central)](https://search.maven.org/artifact/com.github.hroniko/guyver-object-modifier/1.0.0/jar)
### How to use:
--------------
* add dependency to pom
```xml
<dependency>
    <groupId>com.github.hroniko</groupId>
    <artifactId>guyver-object-modifier</artifactId>
    <version>1.0.0</version>
</dependency>
```
* create new Guyver<T> shell from your Object
* modify it (change field values, add new fields)
* extract modified object as guyver.getBody()

### Example Usage:
--------------
```java
// create new object of Test class
    Test test = new Test()
            .setName("MyName")
            .setPercent(true)
            .setCreateDate(new Date().getTime())
            .setCurrencyCode("$")
            .setOrderNumber(1L)
            .setShortName("OOPS")
            .setMinValue("0.0")
            .setMaxValue("INF+")
            .setValue("100.0");

// create new Guyver shell from exist object
    Guyver<Test> guyver = new Guyver<>(test);

// update values for fields of test object with Guyver shell
    guyver.set("name", "Test");
    guyver.set("isPercent", false);
    guyver.set("maxValue", "0.0");

// create new non-exist fields and insert it to test object with Guyver shell
    List<String> list = new ArrayList<>();
    list.add("1");
    list.add("2");
    guyver.addField("ZZZ", list);
    guyver.addField("proto", "Proto");

// get modifiered object from Guyver shell and printing it
    Test test2 = guyver.getBody();
    System.out.println(test2);
    System.out.println(test2.getClass());
```

### Will be printed:
--------------
```java
{"name":"Test","shortName":"OOPS","value":"100.0","currencyCode":"$","isPercent":false,"minValue":"0.0","maxValue":"0.0","createDate":1586257901722,"orderNumber":1,"proto":"Proto","ZZZ":[1, 2]}
class Testv2
```

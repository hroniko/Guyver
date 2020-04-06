package com.hroniko.guyver.modifier;

import com.rits.cloning.Cloner;
import org.joor.Reflect;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Guyver <T> {

    private T originalBody;
    private T body;
    private String className;
    private String packageName;
    private Map<String, String> fieldNameToType;
    private Map<String, Field> fieldNameToField;
    private Cloner cloner;
    private Map<String, String> additionalFieldNameToType;
    private Map<String, Object> additionalFieldNameToValue;

    public Guyver(T body) {
        this.originalBody = body;
        init();
    }

    public Object get(String fieldName){
        return Optional.ofNullable(fieldName)
                .map(fn -> fieldNameToField.get(fn))
                .map(f -> {
                    try {
                        return f.get(body);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .orElse(null);
    }

    public void set(String fieldName, Object fieldValue){
        set(this.body, fieldName, fieldValue);
    }

    private void set(Object tbody, String fieldName, Object fieldValue){
        String type = fieldNameToType.get(fieldName);
        Field field = fieldNameToField.get(fieldName);
        if (type == null || field == null){
            throw new RuntimeException("Field with name " + fieldName + " not found!");
        }

        String valueType = fieldValue.getClass().getTypeName();

        if (!type.equals(valueType)){

            switch (type) {
                case ("boolean"):
                    if (valueType.contains("Boolean")){
                        fieldValue = ((Boolean) fieldValue).booleanValue();
                        break;
                    }
                case ("int"):
                    if (valueType.contains("Integer")){
                        fieldValue = ((Integer) fieldValue).intValue();
                        break;
                    }
                case ("long"):
                    if (valueType.contains("Long")){
                        fieldValue = ((Long) fieldValue).longValue();
                        break;
                    }
                case ("float"):
                    if (valueType.contains("Float")){
                        fieldValue = ((Float) fieldValue).floatValue();
                        break;
                    }
                case ("double"):
                    if (valueType.contains("Double")){
                        fieldValue = ((Double) fieldValue).doubleValue();
                        break;
                    }
                default:
                    throw new RuntimeException("FieldValue has wrong type for filed " + fieldName + "; please use " + type);
            }

        }

        try {
            field.set(tbody, fieldValue);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    public Guyver<T> with(String fieldName, Object fieldValue){
        set(fieldName, fieldValue);
        return this;
    }

    public T getOriginalBody(){
        return this.originalBody;
    }

    public T getBody(){
        if (additionalFieldNameToType.isEmpty() || additionalFieldNameToValue.isEmpty()){
            return this.body;
        } else {
            String imports = additionalFieldNameToType.values().stream()
                    .distinct()
                    .map(x -> "import " + x + ";")
                    .collect(Collectors.joining("\n"));
            String fields = additionalFieldNameToType.entrySet().stream()
                    .map(x -> "private " + x.getValue() + " " + x.getKey() + ";")
                    .collect(Collectors.joining("\n"));
            String toStringExt = "new ToStringBuilder(this, ToStringStyle.JSON_STYLE)\n" +
                    additionalFieldNameToType.keySet().stream()
                            .distinct().map(x -> ".append(\"" + x + "\", " + x  + ")")
                            .collect(Collectors.joining("\n"))
                    + ".toString()";
            Object compile = Reflect.compile(className + "v2",
                    "import org.apache.commons.lang3.builder.ToStringBuilder;\n" +
                            "import org.apache.commons.lang3.builder.ToStringStyle;\n" +
                            imports +
                            "\n" +
                            "public class " + className + "v2" + " extends " + className + "{\n" +
                            fields +
                            "\n" +
                            "    @Override\n" +
                            "    public String toString() {\n" +
                            "        return (super.toString() + " + toStringExt + ").replace(\"}{\", \",\");" +
                            "\n" +
                            "    }\n" +
                            "}").create().get();
            return (T) transposeValues(compile);
        }
    }

    private Object transposeValues(Object compile){

        Guyver<Object> compileGuyver = new Guyver<>(compile);
        if (!fieldNameToField.isEmpty()){
            fieldNameToField.entrySet().forEach(x -> {
                String fieldName = x.getKey();
                Object fieldValue = get(fieldName);
                if (fieldName != null && fieldValue != null){
                    compileGuyver.set(fieldName, fieldValue);
                }
            });
        }

        if (!additionalFieldNameToValue.isEmpty()){
            additionalFieldNameToValue.entrySet().forEach(x -> {
                String fieldName = x.getKey();
                Object fieldValue = x.getValue();
                if (fieldName != null && fieldValue != null){
                    compileGuyver.set(fieldName, fieldValue);
                }
            });
        }

        return compileGuyver.getBody();
    }

    public T clone(){
        return cloner.deepClone(this.body);
    }

    public void setOriginalBody(T originalBody) {
        this.originalBody = originalBody;
        init();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void addField(String fieldName, Object fieldValue){
        String fieldType = fieldValue.getClass().getTypeName();
        additionalFieldNameToType.put(fieldName, fieldType);
        additionalFieldNameToValue.put(fieldName, fieldValue);
    }

    /* ---------------------- Utils ---------------------- */
    private void init(){
        this.cloner = new Cloner();
        this.body = this.cloner.deepClone(this.originalBody);
        this.className = extractClassName(this.body);
        this.packageName = extractPackageName(this.body);
        List<Field> fields = extractFields(this.body);
        this.fieldNameToType = extractFieldNameToTypeMap(fields);
        this.fieldNameToField = extractFieldNameToFieldMap(fields);
        this.additionalFieldNameToType = new HashMap<>();
        this.additionalFieldNameToValue = new HashMap<>();
    }

    private String extractClassName(T body){
        return body.getClass().getSimpleName();
    }

    private String extractPackageName(T body){
        return Optional.ofNullable(body)
                .map((Function<T, ? extends Class<?>>) T::getClass)
                .map(Class::getPackage)
                .map(Package::getName)
                .orElse("");
    }

    private List<Field> extractFields(T body){

        List<Field> fieldsAllParents = new ArrayList<>();
        List<Field> declaredFieldsAllParent = new ArrayList<>();

        Class parent = body.getClass();
        while (parent != null) {
            List<Field> fields = Arrays.asList(parent.getFields());
            fieldsAllParents.addAll(fields);
            List<Field> declaredFields = Arrays.asList(parent.getDeclaredFields());
            declaredFieldsAllParent.addAll(declaredFields);
            parent = parent.getSuperclass();
        }

        return Stream.concat(fieldsAllParents.stream(), declaredFieldsAllParent.stream()).peek(x -> x.setAccessible(true)).collect(Collectors.toList());


    }

    private Map<String, String> extractFieldNameToTypeMap(List<Field> fields){
        return fields.stream()
                .collect(Collectors.toMap(Field::getName, x -> x.getGenericType().toString().replace("class ", "")));
    }

    private Map<String, Field> extractFieldNameToFieldMap(List<Field> fields){
        return fields.stream()
                .collect(Collectors.toMap(Field::getName, x -> x));
    }
}
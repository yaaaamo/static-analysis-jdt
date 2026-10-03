package hai913i.tp1.model;

import java.util.List;
public record TypeFact(String qualifiedName, String packageName, TypeKind kind,
                       List<String> superclasses, List<String> interfaces,
                       List<FieldFact> fields, List<MethodFact> methods) {}

# interlis-transform-engine

```mermaid
sequenceDiagram
    participant App as com.interlis.transform.app.App
    participant Config as com.interlis.transform.mapping.compiler.MappingConfigLoader
    participant Loader as com.interlis.transform.interlis.InterlisModelLoader
    participant Compiler as com.interlis.transform.mapping.compiler.MappingCompiler
    participant Engine as com.interlis.transform.engine.DefaultTransformationEngine
    participant IOFactory as com.interlis.transform.interlis.InterlisIoFactory
    participant Expr as com.interlis.transform.expression.BasicExpressionEngine
    participant Proc as com.interlis.transform.Processor
    participant Reader as ch.interlis.iox.IoxReader
    participant Writer as ch.interlis.iox.IoxWriter

    App->>Config: load(mappingPath)
    App->>Loader: compileModel(modelName, modelDirs)
    App->>Compiler: compile(MappingConfig)
    App->>IOFactory: createReader(inputPath, sourceTD)
    App->>IOFactory: createWriter(outputPath, targetTD)
    App->>Engine: run(reader, writer, plan)

    loop events
        Engine->>Reader: read()
        Engine->>Proc: apply(context)
        Proc->>Expr: evaluate(expr, context)
        Proc-->>Engine: emit target
        Engine-->>Writer: write(IoxEvent)
    end
```

## DSL-Ausdruckssyntax

Die Mapping-Ausdrücke unterstützen eine kleine DSL für Attribute, Literale, Operatoren und Funktionen:

### Literale
- Strings mit einfachen oder doppelten Anführungszeichen, z. B. `'Text'` oder `"Text"`.
- Zahlen wie `42` oder `3.14`.
- Boolesche Werte `true`/`false`.
- `null`.

### Pfade und Kontext
- Quellattribute: `${src.<attr>}` oder `${src.<alias>.<attr>}` für Multi-Source-Mappings.
  - Bei einem Alias wird der erste Teil nach `src.` als Alias interpretiert, wenn er in den bekannten Sources vorhanden ist.
  - Attribute können weitere Punkte enthalten (z. B. strukturierte Attribute).
- Zustandszugriff: `${state.<sourceOid>}` liefert die gemappte Target-OID.

### Operatoren
- Vergleich: `==`, `!=`
- Addition: `+` (numerisch; mehrere Summanden möglich)

### Funktionen
- `substring(value, start, length)`
- `coalesce(a, b, ...)`
- `add(a, b, ...)` (wird intern bei `+` verwendet)
- `if(condition, whenTrue, whenFalse)`
- `to_xml_date(value, pattern?)`
- `to_xml_datetime(value, pattern?)`
- `from_xml_date(value, pattern?)`
- `from_xml_datetime(value, pattern?)`
- `ref(role)` bzw. `ref(alias, role)` löst Rollenreferenzen über OID auf und liefert das referenzierte Objekt (oder `null`).
  - Die Zielklasse wird über ili2c-Metadaten (Viewable/Rolle) ermittelt.
  - Der Lookup verwendet die aktuelle Basket-ID oder eine in der Referenz gespeicherte Basket-ID.

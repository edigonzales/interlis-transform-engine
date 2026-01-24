package com.interlis.transform.interlis;

import ch.interlis.ili2c.metamodel.AreaType;
import ch.interlis.ili2c.metamodel.AttributeDef;
import ch.interlis.ili2c.metamodel.Element;
import ch.interlis.ili2c.metamodel.Model;
import ch.interlis.ili2c.metamodel.SurfaceType;
import ch.interlis.ili2c.metamodel.Topic;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.ili2c.metamodel.Viewable;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.Iom_jObject;
import ch.interlis.iom_j.itf.ItfWriter;
import ch.interlis.iom_j.itf.ModelUtilities;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxFactoryCollection;
import ch.interlis.iox.IoxWriter;
import ch.interlis.iox.StartBasketEvent;
import ch.interlis.iox.StartTransferEvent;
import ch.interlis.iox.EndBasketEvent;
import ch.interlis.iox.EndTransferEvent;
import ch.interlis.iox.ObjectEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SplitGeometryItfWriter implements IoxWriter {
    private final ItfWriter delegate;
    private final TransferDescription transferDescription;
    private final Map<String, List<GeometryAttribute>> geometryAttributesByTag;
    private List<Element> currentItfTables;
    private Map<String, List<BufferedObject>> bufferedObjects;
    private long generatedOidCounter;
    private long maxBaseOid;

    public SplitGeometryItfWriter(File file, TransferDescription transferDescription) throws IoxException {
        this(new ItfWriter(file, transferDescription), transferDescription);
    }

    public SplitGeometryItfWriter(ItfWriter delegate, TransferDescription transferDescription) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.transferDescription = Objects.requireNonNull(transferDescription, "transferDescription");
        this.geometryAttributesByTag = buildGeometryAttributes(transferDescription);
        this.bufferedObjects = new LinkedHashMap<>();
        this.generatedOidCounter = 0L;
        this.maxBaseOid = 0L;
    }

    @Override
    public void write(IoxEvent event) throws IoxException {
        if (event instanceof StartTransferEvent || event instanceof EndTransferEvent) {
            delegate.write(event);
            return;
        }
        if (event instanceof StartBasketEvent) {
            StartBasketEvent startBasket = (StartBasketEvent) event;
            handleStartBasket(startBasket);
            delegate.write(event);
            return;
        }
        if (event instanceof ObjectEvent) {
            ObjectEvent objectEvent = (ObjectEvent) event;
            handleObject(objectEvent.getIomObject());
            return;
        }
        if (event instanceof EndBasketEvent) {
            flushBufferedObjects();
            delegate.write(event);
            resetBasketState();
            return;
        }
        delegate.write(event);
    }

    @Override
    public void close() throws IoxException {
        delegate.close();
    }

    @Override
    public void flush() throws IoxException {
        delegate.flush();
    }

    @Override
    public void setFactory(IoxFactoryCollection factory) throws IoxException {
        delegate.setFactory(factory);
    }

    @Override
    public IoxFactoryCollection getFactory() throws IoxException {
        return delegate.getFactory();
    }

    @Override
    public IomObject createIomObject(String type, String oid) throws IoxException {
        return delegate.createIomObject(type, oid);
    }

    private void handleStartBasket(StartBasketEvent event) {
        String basketType = event.getType();
        String[] parts = basketType == null ? new String[0] : basketType.split("\\.");
        if (parts.length >= 2) {
            currentItfTables = ModelUtilities.getItfTables(transferDescription, parts[0], parts[1]);
        } else {
            currentItfTables = null;
        }
        bufferedObjects = new LinkedHashMap<>();
        generatedOidCounter = 0L;
        maxBaseOid = 0L;
    }

    private void handleObject(IomObject source) {
        String baseOid = source.getobjectoid();
        if (baseOid != null) {
            registerBaseOid(baseOid);
        }
        List<GeometryAttribute> geometryAttributes = geometryAttributesByTag.get(source.getobjecttag());
        if (geometryAttributes == null || geometryAttributes.isEmpty()) {
            bufferObject(tableNameFromTag(source.getobjecttag()), source);
            return;
        }
        Iom_jObject baseObject = new Iom_jObject(source);
        for (GeometryAttribute geometryAttribute : geometryAttributes) {
            String attrName = geometryAttribute.attribute().getName();
            int valueCount = source.getattrvaluecount(attrName);
            for (int index = 0; index < valueCount; index++) {
                IomObject geometryValue = source.getattrobj(attrName, index);
                if (geometryValue == null) {
                    continue;
                }
                String geomAttrName = ModelUtilities.getHelperTableGeomAttrName(geometryAttribute.attribute());
                String geomTag = geometryTagFromSource(source.getobjecttag(), attrName);
                String geometryTag = geometryValue.getobjecttag();
                if ("SURFACE".equalsIgnoreCase(geometryTag) || "MULTISURFACE".equalsIgnoreCase(geometryTag)) {
                    List<IomObject> polylines = extractSurfacePolylines(geometryValue);
                    for (IomObject polyline : polylines) {
                        bufferGeometryObject(
                                tableNameFromGeometry(geometryAttribute.attribute()),
                                geomTag,
                                geomAttrName,
                                polyline,
                                geometryAttribute,
                                baseOid);
                    }
                } else if ("POLYLINE".equalsIgnoreCase(geometryTag) || "MULTIPOLYLINE".equalsIgnoreCase(geometryTag)) {
                    bufferGeometryObject(
                            tableNameFromGeometry(geometryAttribute.attribute()),
                            geomTag,
                            geomAttrName,
                            geometryValue,
                            geometryAttribute,
                            baseOid);
                } else {
                    bufferGeometryObject(
                            tableNameFromGeometry(geometryAttribute.attribute()),
                            geomTag,
                            geomAttrName,
                            geometryValue,
                            geometryAttribute,
                            baseOid);
                }
            }
            for (int index = baseObject.getattrvaluecount(attrName) - 1; index >= 0; index--) {
                baseObject.deleteattrobj(attrName, index);
            }
        }
        bufferObject(tableNameFromTag(source.getobjecttag()), baseObject);
    }

    private void flushBufferedObjects() throws IoxException {
        generatedOidCounter = maxBaseOid;
        if (currentItfTables != null) {
            for (Element element : currentItfTables) {
                String tableName = itfTableName(element);
                List<BufferedObject> tableObjects = bufferedObjects.remove(tableName);
                if (tableObjects == null) {
                    continue;
                }
                for (BufferedObject object : tableObjects) {
                    delegate.write(new ch.interlis.iox_j.ObjectEvent(object.toIomObject()));
                }
            }
        }
        for (List<BufferedObject> remaining : bufferedObjects.values()) {
            for (BufferedObject object : remaining) {
                delegate.write(new ch.interlis.iox_j.ObjectEvent(object.toIomObject()));
            }
        }
        bufferedObjects.clear();
    }

    private void resetBasketState() {
        currentItfTables = null;
        bufferedObjects = new LinkedHashMap<>();
    }

    private void bufferObject(String tableName, IomObject object) {
        bufferedObjects.computeIfAbsent(tableName, key -> new ArrayList<>()).add(new ImmediateObject(object));
    }

    private void bufferGeometryObject(
            String tableName,
            String geomTag,
            String geomAttrName,
            IomObject geometryValue,
            GeometryAttribute geometryAttribute,
            String sourceOid) {
        bufferedObjects.computeIfAbsent(tableName, key -> new ArrayList<>())
                .add(new GeometryObject(geomTag, geomAttrName, geometryValue, geometryAttribute, sourceOid));
    }

    private void registerBaseOid(String baseOid) {
        Long numericOid = tryParseLong(baseOid);
        if (numericOid != null && numericOid > maxBaseOid) {
            maxBaseOid = numericOid;
        }
    }

    private String nextGeneratedOid() {
        generatedOidCounter++;
        return Long.toString(generatedOidCounter);
    }

    private Long tryParseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void addGeometryReference(String sourceOid, GeometryAttribute geometryAttribute, Iom_jObject geometryObject) {
        String refAttrName = ModelUtilities.getHelperTableMainTableRef(geometryAttribute.attribute());
        IomObject ref = geometryObject.addattrobj(refAttrName, Iom_jObject.REF);
        if (sourceOid != null) {
            ref.setobjectrefoid(sourceOid);
        }
    }

    private List<IomObject> extractSurfacePolylines(IomObject geometryValue) {
        List<IomObject> polylines = new ArrayList<>();
        List<IomObject> surfaces = new ArrayList<>();
        if ("SURFACE".equalsIgnoreCase(geometryValue.getobjecttag())) {
            surfaces.add(geometryValue);
        } else {
            int surfaceCount = geometryValue.getattrvaluecount("surface");
            for (int surfaceIndex = 0; surfaceIndex < surfaceCount; surfaceIndex++) {
                IomObject surface = geometryValue.getattrobj("surface", surfaceIndex);
                if (surface != null) {
                    surfaces.add(surface);
                }
            }
        }
        for (IomObject surface : surfaces) {
            int boundaryCount = surface.getattrvaluecount("boundary");
            for (int boundaryIndex = 0; boundaryIndex < boundaryCount; boundaryIndex++) {
                IomObject boundary = surface.getattrobj("boundary", boundaryIndex);
                if (boundary == null) {
                    continue;
                }
                int polylineCount = boundary.getattrvaluecount("polyline");
                for (int polylineIndex = 0; polylineIndex < polylineCount; polylineIndex++) {
                    IomObject polyline = boundary.getattrobj("polyline", polylineIndex);
                    if (polyline != null) {
                        polylines.add(polyline);
                    }
                }
            }
        }
        return polylines;
    }

    private Map<String, List<GeometryAttribute>> buildGeometryAttributes(TransferDescription transferDescription) {
        Map<String, List<GeometryAttribute>> mapping = new HashMap<>();
        Iterator<?> modelIterator = transferDescription.iterator();
        while (modelIterator.hasNext()) {
            Object modelObj = modelIterator.next();
            if (!(modelObj instanceof Model)) {
                continue;
            }
            Model model = (Model) modelObj;
            Iterator<?> topicIterator = model.iterator();
            while (topicIterator.hasNext()) {
                Object topicObj = topicIterator.next();
                if (!(topicObj instanceof Topic)) {
                    continue;
                }
                Topic topic = (Topic) topicObj;
                for (Object viewableObj : topic.getViewables()) {
                    if (!(viewableObj instanceof Viewable)) {
                        continue;
                    }
                    Viewable viewable = (Viewable) viewableObj;
                    Iterator<?> attrIterator = viewable.getAttributes();
                    List<GeometryAttribute> geometryAttributes = new ArrayList<>();
                    while (attrIterator.hasNext()) {
                        Object attrObj = attrIterator.next();
                        if (!(attrObj instanceof AttributeDef)) {
                            continue;
                        }
                        AttributeDef attr = (AttributeDef) attrObj;
                        Type realType = Type.findReal(attr.getDomain());
                        if (realType instanceof AreaType) {
                            geometryAttributes.add(new GeometryAttribute(attr, false));
                        } else if (realType instanceof SurfaceType) {
                            geometryAttributes.add(new GeometryAttribute(attr, true));
                        }
                    }
                    if (!geometryAttributes.isEmpty()) {
                        mapping.put(viewable.getScopedName(null), geometryAttributes);
                    }
                }
            }
        }
        return mapping;
    }

    private String tableNameFromTag(String objectTag) {
        String[] parts = objectTag.split("\\.");
        return parts.length == 0 ? objectTag : parts[parts.length - 1];
    }

    private String geometryTagFromSource(String objectTag, String attrName) {
        return objectTag + "_" + attrName;
    }

    private String tableNameFromGeometry(AttributeDef attribute) {
        Viewable viewable = (Viewable) attribute.getContainer();
        return viewable.getName() + "_" + attribute.getName();
    }

    private String itfTableName(Element element) {
        if (element instanceof AttributeDef) {
            AttributeDef attribute = (AttributeDef) element;
            Viewable viewable = (Viewable) attribute.getContainer();
            return viewable.getName() + "_" + attribute.getName();
        }
        return element.getName();
    }

    private interface BufferedObject {
        IomObject toIomObject();
    }

    private record ImmediateObject(IomObject object) implements BufferedObject {
        @Override
        public IomObject toIomObject() {
            return object;
        }
    }

    private final class GeometryObject implements BufferedObject {
        private final String geomTag;
        private final String geomAttrName;
        private final IomObject geometryValue;
        private final GeometryAttribute geometryAttribute;
        private final String sourceOid;

        private GeometryObject(
                String geomTag,
                String geomAttrName,
                IomObject geometryValue,
                GeometryAttribute geometryAttribute,
                String sourceOid) {
            this.geomTag = geomTag;
            this.geomAttrName = geomAttrName;
            this.geometryValue = geometryValue;
            this.geometryAttribute = geometryAttribute;
            this.sourceOid = sourceOid;
        }

        @Override
        public IomObject toIomObject() {
            String generatedOid = nextGeneratedOid();
            Iom_jObject geometryObject = new Iom_jObject(geomTag, generatedOid);
            geometryObject.addattrobj(geomAttrName, geometryValue);
            if (geometryAttribute.isSurface()) {
                addGeometryReference(sourceOid, geometryAttribute, geometryObject);
            }
            return geometryObject;
        }
    }

    private record GeometryAttribute(AttributeDef attribute, boolean isSurface) {}
}

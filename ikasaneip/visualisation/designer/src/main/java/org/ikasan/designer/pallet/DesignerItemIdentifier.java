package org.ikasan.designer.pallet;

import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

public class DesignerItemIdentifier {
    public static final String NOT_APPLICABLE = "NOT_APPLICABLE";

    String type;
    String name;
    String uuid;

    public DesignerItemIdentifier(String type, String name, String uuid) {
        this.type = type;
        this.name = name;
        this.uuid = uuid;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.getType()).append(":");
        stringBuilder.append(this.getName()).append(":");
        stringBuilder.append(this.getUuid());

        return stringBuilder.toString();
    }

    public static DesignerItemIdentifier getIdentifier(String compositeIdentifierString) {
        List<String> tokens = Collections.list(new StringTokenizer(compositeIdentifierString, ":")).stream()
            .map(token -> (String) token)
            .collect(Collectors.toList());

        if(tokens.size() != 3){
            throw new IllegalArgumentException("The composite identifier does not contain enough tokens!");
        }

        return new DesignerItemIdentifier(tokens.get(0), tokens.get(1), tokens.get(2));
    }
}

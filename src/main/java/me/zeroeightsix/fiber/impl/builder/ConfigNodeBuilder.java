package me.zeroeightsix.fiber.impl.builder;

import me.zeroeightsix.fiber.api.FiberId;
import me.zeroeightsix.fiber.api.tree.ConfigAttribute;
import me.zeroeightsix.fiber.api.tree.ConfigNode;
import me.zeroeightsix.fiber.api.tree.ConfigTree;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class ConfigNodeBuilder {
    @Nullable
    protected ConfigTree parent;
    @Nullable
    protected String name;
    @Nullable
    protected String comment = null;
    protected Map<FiberId, ConfigAttribute<?>> attributes;

    public ConfigNodeBuilder(@Nullable ConfigTree parent, @Nullable String name) {
        if (parent != null && name == null) throw new IllegalArgumentException("A child node needs a name");
        this.parent = parent;
        this.name = name;
        this.attributes = new HashMap<>();
    }

    /**
     * Sets the {@code ConfigNode}'s name.
     *
     * @param name the name
     * @return {@code this} builder
     * @see ConfigTree#lookup
     */
    public ConfigNodeBuilder withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the {@code ConfigNode}'s comment.
     *
     * <p> If {@code null}, or if this method is never called, the {@code ConfigNode} will not have a comment.
     * An empty comment (non null, but only consisting of whitespace) will be serialised.
     *
     * @param comment the comment
     * @return {@code this} builder
     */
    public ConfigNodeBuilder withComment(String comment) {
        this.comment = comment;
        return this;
    }

    public <A> ConfigNodeBuilder withAttribute(FiberId id, Class<A> type, A defaultValue) {
        return this.withAttribute(ConfigAttribute.create(id, type, defaultValue));
    }

    public ConfigNodeBuilder withAttributes(Collection<ConfigAttribute<?>> attributes) {
        for (ConfigAttribute<?> attribute : attributes) {
            this.withAttribute(attribute);
        }
        return this;
    }

    public ConfigNodeBuilder withAttribute(ConfigAttribute<?> attribute) {
        this.attributes.put(attribute.getIdentifier(), attribute);
        return this;
    }

    public abstract ConfigNode build();
}

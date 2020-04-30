package me.zeroeightsix.fiber.impl.tree;

import java.util.Objects;

import javax.annotation.Nullable;

import me.zeroeightsix.fiber.api.schema.type.derived.ConfigType;
import me.zeroeightsix.fiber.api.tree.ConfigLeaf;
import me.zeroeightsix.fiber.api.tree.Property;
import me.zeroeightsix.fiber.api.tree.PropertyMirror;

public final class PropertyMirrorImpl<R, S> implements PropertyMirror<R> {
    protected Property<S> delegate;
    protected ConfigType<R, S, ?> converter;
    @Nullable
    private S lastSerializedValue;
    @Nullable
    private R cachedValue;

    public PropertyMirrorImpl(ConfigType<R, S, ?> converter) {
        this.converter = converter;
    }

    /**
     * Sets a property to mirror.
     *
     * <p>After calling this method with a valid delegate,
     * every property method will redirect to {@code delegate}.
     *
     * @param delegate a property to mirror
     */
    @Override
    public void mirror(Property<?> delegate) {
        if (!this.converter.getSerializedType().getPlatformType().equals(delegate.getType())) {
            throw new IllegalArgumentException("Unsupported delegate type " + delegate.getType() + ", should be " + this.converter.getSerializedType().getPlatformType());
        }

        @SuppressWarnings("unchecked") Property<S> d = (Property<S>) delegate;
        this.delegate = d;

        if (d instanceof ConfigLeaf) {
            // passive invalidation
            ((ConfigLeaf<S>) d).addChangeListener((old, cur) -> this.cachedValue = null);
            this.lastSerializedValue = null;
        } else {
            // active invalidation, less efficient
            this.lastSerializedValue = d.getValue();
        }
    }

    @Override
    public Property<?> getMirrored() {
        return this.delegate;
    }

    @Override
    public boolean setValue(R value) {
        if (this.delegate == null) throw new IllegalStateException("No delegate property set for this mirror");
        return this.delegate.setValue(this.converter.toPlatformType(value));
    }

    @Override
    public boolean accepts(R value) {
        if (this.delegate == null) throw new IllegalStateException("No delegate property set for this mirror");
        return this.delegate.accepts(this.converter.toPlatformType(value));
    }

    @Override
    public R getValue() {
        if (this.delegate == null) throw new IllegalStateException("No delegate property set for this mirror");

        if (this.cachedValue == null || this.lastSerializedValue != null) {
            S serializedValue = this.delegate.getValue();

            if (!Objects.equals(this.lastSerializedValue, serializedValue)) {
                this.cachedValue = this.converter.toRuntimeType(serializedValue);
                this.lastSerializedValue = serializedValue;
            }
        }

        return this.cachedValue;
    }

    @Override
    public Class<R> getType() {
        return this.converter.getRuntimeType();
    }

    @Override
    public ConfigType<R, S, ?> getConverter() {
        return this.converter;
    }
}

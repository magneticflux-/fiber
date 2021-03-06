package io.github.fablabsmc.fablabs.impl.fiber.tree;

import java.util.Objects;
import java.util.function.BiConsumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.fablabsmc.fablabs.api.fiber.v1.builder.ConfigLeafBuilder;
import io.github.fablabsmc.fablabs.api.fiber.v1.schema.type.SerializableType;
import io.github.fablabsmc.fablabs.api.fiber.v1.schema.type.TypeCheckResult;
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.ConfigLeaf;

public final class ConfigLeafImpl<T> extends ConfigNodeImpl implements ConfigLeaf<T> {
	private T value;
	@Nonnull
	private final T defaultValue;
	@Nonnull
	private BiConsumer<T, T> listener;
	@Nonnull
	private final SerializableType<T> type;

	/**
	 * Creates a {@code ConfigLeaf}.
	 *
	 * @param name         the name for this node
	 * @param type         the type of value this item holds
	 * @param comment      the comment for this node
	 * @param defaultValue the default value for this node
	 * @param listener     the consumer or listener for this item. When this item's value changes, the consumer will be called with the old value as first argument and the new value as second argument.
	 * @see ConfigLeafBuilder
	 */
	public ConfigLeafImpl(@Nonnull String name, @Nonnull SerializableType<T> type, @Nullable String comment, @Nonnull T defaultValue, @Nonnull BiConsumer<T, T> listener) {
		super(name, comment);
		this.defaultValue = Objects.requireNonNull(defaultValue);
		this.listener = listener;
		this.type = type;
		this.setValue(defaultValue);
	}

	@Override
	@Nonnull
	public T getValue() {
		return this.value;
	}

	@Override
	public SerializableType<T> getConfigType() {
		return this.type;
	}

	@Override
	public boolean accepts(@Nonnull T value) {
		return this.type.accepts(value);
	}

	@Override
	public boolean setValue(@Nonnull T value) {
		T correctedValue;
		TypeCheckResult<T> result = this.type.test(value);

		if (result.hasPassed()) {
			correctedValue = value;
		} else {
			if (!result.getCorrectedValue().isPresent()) {
				return false;
			}

			correctedValue = result.getCorrectedValue().get();
		}

		T oldValue = this.value;
		this.value = Objects.requireNonNull(correctedValue);
		this.listener.accept(oldValue, this.value);
		return true;
	}

	@Override
	@Nonnull
	public BiConsumer<T, T> getListener() {
		return listener;
	}

	@Override
	public void addChangeListener(BiConsumer<T, T> listener) {
		this.listener = this.listener.andThen(listener);
	}

	@Override
	@Nonnull
	public T getDefaultValue() {
		return defaultValue;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName()
				+ '<' + this.type.getGenericPlatformType().getTypeName()
				+ ">[name=" + this.getName()
				+ ", comment=" + this.getComment()
				+ ", value=" + this.getValue()
				+ "]";
	}
}

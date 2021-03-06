package io.github.fablabsmc.fablabs.api.fiber.v1.schema.type.derived;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.github.fablabsmc.fablabs.api.fiber.v1.builder.ConfigLeafBuilder;
import io.github.fablabsmc.fablabs.api.fiber.v1.exception.FiberConversionException;
import io.github.fablabsmc.fablabs.api.fiber.v1.schema.type.DecimalSerializableType;
import io.github.fablabsmc.fablabs.api.fiber.v1.schema.type.ListSerializableType;
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.ConfigLeaf;
import io.github.fablabsmc.fablabs.api.fiber.v1.tree.PropertyMirror;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfigTypesTest {
	@DisplayName("Test numerical constraints")
	@Test
	void testNumericalConstraints() {
		assertThrows(IllegalStateException.class, () -> ConfigTypes.UNBOUNDED_DECIMAL.withIncrement(BigDecimal.valueOf(5)), "Increment without a minimum");
		assertThrows(IllegalArgumentException.class, () -> ConfigTypes.INTEGER.withIncrement(-5), "Negative increment");

		DecimalSerializableType type = ConfigTypes.NATURAL.withMinimum(10).withMaximum(20).getSerializedType();

		Predicate<Integer> finalConstraint = i -> type.accepts(BigDecimal.valueOf(i));

		assertEquals(BigDecimal.TEN, type.getMinimum(), "Correct minimum");
		assertEquals(BigDecimal.valueOf(20), type.getMaximum(), "Correct maximum");

		assertFalse(finalConstraint.test(-2), "Input can't be lower than 10");
		assertFalse(finalConstraint.test(4), "Input can't be lower than 10");

		assertTrue(finalConstraint.test(15), "Input can be between 10 and 20");

		assertFalse(finalConstraint.test(25), "Input can't be above 20");
		assertFalse(type.accepts(BigDecimal.valueOf(0.5)));
	}

	@DisplayName("Test array type constraints")
	@Test
	void testArrayConstraints() {
		DecimalSerializableType elementType = ConfigTypes.INTEGER.withValidRange(3, 10, 2).getSerializedType();
		ListSerializableType<BigDecimal> type = new ListSerializableType<>(elementType, 0, 3, false);
		Predicate<Integer[]> config = arr -> type.accepts(Arrays.stream(arr).map(BigDecimal::valueOf).collect(Collectors.toList()));

		assertTrue(config.test(new Integer[0]));
		assertTrue(config.test(new Integer[] {4, 4, 6}));
		assertFalse(config.test(new Integer[] {4, 5, 6}));
		assertFalse(config.test(new Integer[] {1, 2}));
		assertFalse(config.test(new Integer[] {5, 6, 7, 8}));
		assertFalse(config.test(new Integer[] {9, 10, 11}));
	}

	@DisplayName("Test collection type constraints")
	@Test
	void testCollectionConstraints() {
		NumberConfigType<Integer> elementType = ConfigTypes.INTEGER.withMinimum(3).withMaximum(10);
		ListConfigType<List<Integer>, BigDecimal> type = ConfigTypes.makeList(elementType).withMaxSize(3);
		Assertions.assertEquals(elementType.getSerializedType(), type.getSerializedType().getElementType());
		ConfigLeaf<List<BigDecimal>> config = ConfigLeafBuilder
				.create(null, "", type, Collections.emptyList())
				.build();
		PropertyMirror<List<Integer>> mirror = PropertyMirror.create(type);
		mirror.mirror(config);

		assertTrue(config.accepts(Collections.emptyList()));
		assertTrue(mirror.accepts(Collections.emptyList()));
		assertTrue(mirror.accepts(Arrays.asList(4, 5, 6)));
		assertFalse(mirror.accepts(Arrays.asList(1, 2)));
		assertFalse(mirror.accepts(Arrays.asList(5, 6, 7, 8)));
		assertFalse(mirror.accepts(Arrays.asList(9, 10, 11)));
	}

	@Test
	void testIntArray() {
		ListConfigType<int[], BigDecimal> type = ConfigTypes.makeIntArray(ConfigTypes.INTEGER);
		int[] arr = {1, 2, 3, 4};
		List<BigDecimal> ls = Arrays.stream(arr).mapToObj(BigDecimal::valueOf).collect(Collectors.toList());
		List<BigDecimal> err = Collections.singletonList(BigDecimal.valueOf(2L * Integer.MAX_VALUE));
		assertEquals(ls, type.toSerializedType(arr), "Convert int[] -> List<BigDecimal>");
		assertArrayEquals(arr, type.toRuntimeType(ls), "Convert List<BigDecimal> -> int[]");
		assertThrows(FiberConversionException.class, () -> type.toRuntimeType(err), "Convert List<overflowed> -> int[]");
	}

	@Test
	void testCharArray() {
		ListConfigType<char[], String> type = ConfigTypes.makeCharArray(ConfigTypes.CHARACTER);
		char[] arr = {'a', 'b', 'c', 'd'};
		List<String> ls = Arrays.asList("a", "b", "c", "d");
		List<String> err = Arrays.asList("", "aa");
		assertEquals(ls, type.toSerializedType(arr), "Convert char[] -> List<String>");
		assertArrayEquals(arr, type.toRuntimeType(ls), "Convert List<String> -> char[]");
		assertThrows(FiberConversionException.class, () -> type.toRuntimeType(err), "Convert List<bad lengths> -> char[]");
	}

	@Test
	void testBooleanArray() {
		ListConfigType<boolean[], Boolean> type = ConfigTypes.makeBooleanArray(ConfigTypes.BOOLEAN);
		boolean[] arr = {false, true};
		List<Boolean> ls = Arrays.asList(false, true);
		assertEquals(ls, type.toSerializedType(arr), "Convert boolean[] -> List<Boolean>");
		assertArrayEquals(arr, type.toRuntimeType(ls), "Convert List<Boolean> -> boolean[]");
	}

	@Test
	void testObjArray() {
		ListConfigType<String[], String> type = ConfigTypes.makeArray(ConfigTypes.STRING);
		String[] arr = {"string1", "string2", "string3", "string4"};
		List<String> ls = Arrays.asList(arr.clone());
		assertEquals(ls, type.toSerializedType(arr), "Convert String[] -> List<String>");
		assertArrayEquals(arr, type.toRuntimeType(ls), "Convert List<String> -> String[]");
	}

	enum TestEnum { A, B }

	@Test
	void testEnum() {
		EnumConfigType<TestEnum> type = ConfigTypes.makeEnum(TestEnum.class);
		Predicate<TestEnum> constraint = a -> type.getSerializedType().accepts(type.toSerializedType(a));
		assertTrue(constraint.test(TestEnum.A), "Unconstrained enum accepts A");
		assertTrue(constraint.test(TestEnum.B), "Unconstrained enum accepts B");

		EnumConfigType<TestEnum> constrainedType = type.withValues(TestEnum.A);
		constraint = a -> constrainedType.getSerializedType().accepts(type.toSerializedType(a));
		assertTrue(constraint.test(TestEnum.A), "Constrained enum 1 enum accepts A");
		assertFalse(constraint.test(TestEnum.B), "Constrained enum 1 does not accept B");

		EnumConfigType<TestEnum> constrainedType2 = type.withValues(Collections.singleton(TestEnum.B));
		constraint = a -> constrainedType2.getSerializedType().accepts(type.toSerializedType(a));
		assertFalse(constraint.test(TestEnum.A), "Constrained enum 2 does not accept A");
		assertTrue(constraint.test(TestEnum.B), "Constrained enum 2 accepts B");
	}
}

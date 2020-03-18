package me.zeroeightsix.fiber.annotation;

import me.zeroeightsix.fiber.exception.FiberException;
import me.zeroeightsix.fiber.tree.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

class AnnotatedSettingsTest {

    private ConfigNode node;

    @BeforeEach
    void setup() {
        node = new ConfigNode();
    }

    @Test
    @DisplayName("Convert POJO to IR")
    void testPojoIR() throws FiberException {
        OneFieldPojo pojo = new OneFieldPojo();
        AnnotatedSettings.applyToNode(node, pojo);

        Set<TreeItem> items = node.getItems();
        assertEquals(1, items.size(), "Setting map is 1 entry large");
        TreeItem item = node.lookup("a");
        assertNotNull(item, "Setting exists");
        assertTrue(ConfigValue.class.isAssignableFrom(item.getClass()), "Setting is a ConfigValue");
        ConfigValue<?> configValue = (ConfigValue<?>) item;
        assertNotNull(configValue.getValue(), "Setting value is non-null");
        assertEquals(Integer.class, configValue.getType(), "Setting type is correct");
        assertEquals(Integer.class, configValue.getValue().getClass(), "Setting value reflects correct type");
        Integer integer = (Integer) configValue.getValue();
        assertEquals(integer, 5, "Setting value is correct");
    }

    @Test
    @DisplayName("Throw final exception")
    void testNoFinal() {
        FinalSettingPojo pojo = new FinalSettingPojo();
        assertThrows(FiberException.class, () -> AnnotatedSettings.applyToNode(node, pojo));
    }

    @Test
    @DisplayName("Listener")
    void testListener() throws FiberException {
        ListenerPojo pojo = new ListenerPojo();
        AnnotatedSettings.applyToNode(node, pojo);

        TreeItem treeItem = node.lookup("a");
        assertNotNull(treeItem, "Setting A exists");
        assertTrue(treeItem instanceof Property<?>, "Setting A is a property");
        @SuppressWarnings("unchecked")
        Property<Integer> property = (Property<Integer>) treeItem;
        property.setValue(10);
        assertTrue(pojo.listenedA, "Listener for A was triggered");

        treeItem = node.lookup("b");
        assertNotNull(treeItem, "Setting B exists");
        assertTrue(treeItem instanceof Property<?>, "Setting B is a property");
        property = (Property<Integer>) treeItem;
        property.setValue(10);
        assertTrue(pojo.listenedB, "Listener for B was triggered");

        treeItem = node.lookup("c");
        assertNotNull(treeItem, "Setting C exists");
        assertTrue(treeItem instanceof Property<?>, "Setting C is a property");
        property = (Property<Integer>) treeItem;
        property.setValue(10);
        assertTrue(pojo.listenedC, "Listener for C was triggered");
    }

    @Test
    @DisplayName("Listener with different generics")
    void testTwoGenerics() {
        NonMatchingListenerPojo pojo = new NonMatchingListenerPojo();
        assertThrows(FiberException.class, () -> AnnotatedSettings.applyToNode(node, pojo));
    }

    @Test
    @DisplayName("Listener with wrong generic type")
    void testWrongGenerics() {
        WrongGenericListenerPojo pojo = new WrongGenericListenerPojo();
        assertThrows(FiberException.class, () -> AnnotatedSettings.applyToNode(node, pojo));
    }

    @Test
    @DisplayName("Numerical constraints")
    void testNumericalConstraints() throws FiberException {
        NumericalConstraintsPojo pojo = new NumericalConstraintsPojo();
        AnnotatedSettings.applyToNode(node, pojo);
        @SuppressWarnings("unchecked")
        Property<Integer> value = (Property<Integer>) node.lookup("a");
        assertNotNull(value, "Setting exists");
        assertFalse(value.setValue(-10));
        assertTrue(value.setValue(5));
        assertFalse(value.setValue(20));
    }

    @Test
    @DisplayName("String constraints")
    void testStringConstraints() throws FiberException {
        StringConstraintsPojo pojo = new StringConstraintsPojo();
        AnnotatedSettings.applyToNode(node, pojo);
        @SuppressWarnings("unchecked")
        Property<String> value = (Property<String>) node.lookup("a");
        assertNotNull(value, "Setting exists");
        assertFalse(value.setValue("BAD STRING::"));
        assertTrue(value.setValue("good:string"));
        assertFalse(value.setValue("b:s"), "Too short");
        assertFalse(value.setValue("bad_string:because_it_is_way_too_long"), "Too long");
    }

    @Test
    @DisplayName("Array constraints")
    void testArrayConstraints() throws FiberException {
        ArrayConstraintsPojo pojo = new ArrayConstraintsPojo();
        AnnotatedSettings.applyToNode(node, pojo);
        @SuppressWarnings("unchecked")
        Property<String[]> value1 = (Property<String[]>) node.lookup("nonEmptyArrayShortStrings");
        assertNotNull(value1, "Setting exists");
        assertTrue(value1.setValue(new String[]{"ab", "", "ba", ""}));
        assertFalse(value1.setValue(new String[0]), "Empty array");
        assertFalse(value1.setValue(new String[]{"aaaaaaaaaaaa"}), "Strings too long");
        @SuppressWarnings("unchecked")
        Property<int[]> value2 = (Property<int[]>) node.lookup("numbers");
        assertNotNull(value2, "Setting exists");
        assertTrue(value2.setValue(new int[]{3, 4, 5}));
        assertTrue(value2.setValue(new int[0]));
        assertFalse(value2.setValue(new int[]{1, 2, 3, 4, 5, 6, 7}), "Too many elements");
        assertFalse(value2.setValue(new int[]{-1, 0, 1}), "Negative number not allowed");
        assertFalse(value2.setValue(new int[]{9, 10, 11}), "Numbers above 10 not allowed");
        @SuppressWarnings("unchecked")
        Property<List<String>> value3 = (Property<List<String>>) node.lookup("shortArrayIdStrings");
        assertNotNull(value3, "Setting exists");
        assertTrue(value3.setValue(Arrays.asList("a:b", "fabric:test")));
        assertTrue(value3.setValue(Collections.emptyList()));
        assertFalse(value3.setValue(Arrays.asList("a:b", "b:c", "c:d", "d:e")), "Too many elements");
        assertFalse(value3.setValue(Collections.singletonList("aaaaaaaaaaaa")), "Bad regex");
    }

    @Test
    @DisplayName("Only annotated fields")
    void testOnlyAnnotatedFields() throws FiberException {
        OnlyAnnotatedFieldsPojo pojo = new OnlyAnnotatedFieldsPojo();
        AnnotatedSettings.applyToNode(node, pojo);
        assertEquals(1, node.getItems().size(), "Node has one item");
    }

    @Test
    @DisplayName("Custom named setting")
    void testCustomNames() throws FiberException {
        CustomNamePojo pojo = new CustomNamePojo();
        AnnotatedSettings.applyToNode(node, pojo);
        assertNotNull(node.lookup("custom_name"), "Custom named setting exists");
    }

    @Test
    @DisplayName("Constant setting")
    void testConstantSetting() throws FiberException {
        ConstantSettingPojo pojo = new ConstantSettingPojo();
        AnnotatedSettings.applyToNode(node, pojo);
        assertFalse(((ConfigValue<Integer>) node.lookup("a")).setValue(0));
    }

    @Test
    @DisplayName("Subnodes")
    void testSubNodes() throws FiberException {
        SubNodePojo pojo = new SubNodePojo();
        AnnotatedSettings.applyToNode(node, pojo);
        assertEquals(1, node.getItems().size(), "Node has one item");
        Node subnode = (Node) node.lookup("a");
        assertNotNull(subnode, "Subnode exists");
        assertEquals(1, subnode.getItems().size(), "Subnode has one item");
    }

    @Test
    @DisplayName("Commented setting")
    @SuppressWarnings("unchecked")
    void testComment() throws FiberException {
        CommentPojo pojo = new CommentPojo();
        AnnotatedSettings.applyToNode(node, pojo);
        assertEquals("comment", ((ConfigValue<Integer>) node.lookup("a")).getComment(), "Comment exists and is correct");
    }

    @Test
    @DisplayName("Ignored settings")
    void testIgnore() throws FiberException {
        IgnoredPojo pojo = new IgnoredPojo();
        AnnotatedSettings.applyToNode(node, pojo);
        assertEquals(0, node.getItems().size(), "Node is empty");
    }

    private static class FinalSettingPojo {
        private final int a = 5;
    }

    private static class OneFieldPojo {
        private int a = 5;
    }

    private static class ListenerPojo {
        private transient boolean listenedA = false;
        private transient boolean listenedB = false;
        private transient boolean listenedC = false;

        private int a = 5;
        private int b = 5;
        private int c = 5;

        @Listener("a")
        private BiConsumer<Integer, Integer> aListener = (now, then) -> listenedA = true;

        @Listener("b")
        private void bListener(Integer oldValue, Integer newValue) {
            listenedB = true;
        }

        @Listener("c")
        private void cListener(Integer newValue) {
            listenedC = true;
        }
    }

    private static class NonMatchingListenerPojo {
        private int a = 5;

        @Listener("a")
        private BiConsumer<Double, Integer> aListener = (now, then) -> {};
    }

    private static class WrongGenericListenerPojo {
        private int a = 5;

        @Listener("a")
        private BiConsumer<Double, Double> aListener = (now, then) -> {};
    }

    private static class NumericalConstraintsPojo {
        @Setting.Constrain.BiggerThan(0)
        @Setting.Constrain.SmallerThan(10)
        private int a = 5;
    }

    private static class StringConstraintsPojo {
        @Setting.Constrain.MinLength(5)
        @Setting.Constrain.MaxLength(20)
        @Setting.Constrain.Regex("[a-z0-9_.-]{2,}:[a-z0-9_./-]+?")
        private String a = "fabric:test";
    }

    private static class ArrayConstraintsPojo {
        @Setting.Constrain.MinLength(1)
        private String @Setting.Constrain.MaxLength(2)[] nonEmptyArrayShortStrings = {""};

        @Setting.Constrain.MinLength(0)
        @Setting.Constrain.MaxLength(3)
        private int @Setting.Constrain.BiggerThan(0) @Setting.Constrain.SmallerThan(10)[] numbers = {};

        @Setting.Constrain.MaxLength(3)
        private List<@Setting.Constrain.Regex("\\w+:\\w+") String> shortArrayIdStrings = Collections.singletonList("fabric:test");
    }

    @Settings(onlyAnnotated = true)
    private static class OnlyAnnotatedFieldsPojo {
        @Setting
        private int a = 5;

        private int b = 6;
    }

    private static class CustomNamePojo {
        @Setting(name = "custom_name")
        private int a = 5;
    }

    private static class ConstantSettingPojo {
        @Setting(constant = true)
        private int a = 5;
    }

    private static class CommentPojo {
        @Setting(comment = "comment")
        private int a = 5;
    }

    private static class IgnoredPojo {
        @Setting(ignore = true)
        private int a = 5;

        private transient int b = 5;
    }

    private static class SubNodePojo {
        @Setting.Node(name = "a")
        public SubNode node = new SubNode();

        class SubNode {
            private int b = 5;
        }
    }

}

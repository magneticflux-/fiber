import me.zeroeightsix.fiber.ir.ConfigNode;
import me.zeroeightsix.fiber.ir.ConfigValue;
import me.zeroeightsix.fiber.pojo.PojoSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PojoTest {

    @Test
    @DisplayName("Convert POJO to IR")
    public void testPojoIR() throws IllegalAccessException {
        ConfigNode node = new ConfigNode();
        OneFieldPojo pojo = new OneFieldPojo();
        PojoSettings.applyToIR(node, pojo);

        Map<String, ConfigValue> settingMap = node.getSettingsImmutable();
        assertEquals(1, settingMap.size(), "Setting map is 1 entry large");
        ConfigValue value = settingMap.get("a");
        assertNotNull(value, "Setting exists");
        assertNotNull(value.getValue(), "Setting value is non-null");
        assertEquals(Integer.class, value.getType(), "Setting type is correct");
        assertEquals(Integer.class, value.getValue().getClass(), "Setting value reflects correct type");
        Integer integer = (Integer) value.getValue();
        assertEquals(integer, 5);
    }

    @Test
    @DisplayName("Throw no final exception")
    public void testNoFinal() {
        ConfigNode node = new ConfigNode();
        NoFinalPojo pojo = new NoFinalPojo();
        assertThrows(IllegalStateException.class, () -> PojoSettings.applyToIR(node, pojo));
    }

    private static class NoFinalPojo {
        private int a = 5;
    }

    private static class OneFieldPojo {
        private final int a = 5;
    }
}

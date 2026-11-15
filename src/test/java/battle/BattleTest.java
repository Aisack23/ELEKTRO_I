package battle;

import entities.Entity;
import enums.AttackSpeed;
import enums.StateOfBattle;
import items.consumables.gameHealings.*;
import items.weapons.playerWeapons.Bat;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class BattleTest {
    private Entity entity(int hp) {
        return new Entity(hp, AttackSpeed.values()[0], new Bat(10, 100)) { };
    }
    private Battle battle(Entity player, Entity enemy) {
        return new Battle(StateOfBattle.readyAndGo, player, enemy, false, "", null);
    }
    @Test void invalidAttackFailsPromptlyWithoutDamagingEitherEntity() {
        Entity player = entity(100), enemy = entity(100);
        var battle = battle(player, enemy);
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            assertThrows(IllegalArgumentException.class, () -> battle.playerAttack(99));
            assertThrows(IllegalArgumentException.class, () -> battle.fight(99));
        });
        assertEquals(100, player.getHP());
        assertEquals(100, enemy.getHP());
    }
    @Test void healingCapsHealthConsumesOnlyMatchingItemAndDoesNotMutateIt() {
        Entity player = entity(95);
        var food = new Food(10);
        player.getHealingsItems().add(new Bandage(25));
        player.getHealingsItems().add(food);
        var battle = battle(player, entity(100));
        battle.foodHealing();
        assertEquals(100, player.getHP());
        assertEquals(10, food.getHealing());
        assertEquals(1, player.getHealingsItems().size());
        battle.bandageHealing(); // full health preserves inventory
        assertEquals(1, player.getHealingsItems().size());
        player.setHP(5);
        battle.kitHealing(); // missing item is a normal no-op
        assertEquals(5, player.getHP());
        battle.bandageHealing();
        assertEquals(30, player.getHP());
        player.getHealingsItems().add(new Kit(50));
        battle.kitHealing();
        assertEquals(80, player.getHP());
        assertTrue(player.getHealingsItems().isEmpty());
    }
}

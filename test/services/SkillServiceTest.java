package services;

import item.Template.SkillTemplate;
import org.junit.jupiter.api.Test;
import player.Player;
import skill.Skill;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillServiceTest {
    @Test
    void percentManaCostDoesNotOverflowAtHighKi() {
        Player player = new Player();
        Skill skill = new Skill();
        skill.template = new SkillTemplate();
        skill.template.id = Skill.SUPER_KAME;
        skill.template.manaUseType = 1;
        skill.manaUse = 80;
        player.playerSkill.skillSelect = skill;
        player.nPoint.mpMax = 2_000_000_000;

        player.nPoint.mp = 1_599_999_999;
        assertFalse(SkillService.gI().canUseSkillWithMana(player));
        player.nPoint.mp = 1_600_000_000;
        assertTrue(SkillService.gI().canUseSkillWithMana(player));
    }
}

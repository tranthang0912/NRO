package web;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import player.Player;
import skill.Skill;
import item.Template.SkillTemplate;
class PlayerViewTest {
    @Test void serializesUnlearnedSkillWithMissingLegacyDisplayMetadata(){
        Player p=new Player();Skill skill=new Skill();skill.template=new SkillTemplate();skill.template.id=19;p.playerSkill.skills.add(skill);
        String json=assertDoesNotThrow(()->GameWorld.JSON.toJson(PlayerView.stats(p)));
        assertTrue(json.contains("Kỹ năng 19"));assertTrue(json.contains("\"level\":0"));
    }
}

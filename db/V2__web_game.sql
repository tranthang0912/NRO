-- Preserve numeric template indexes while retiring paid progression.
INSERT INTO achievement_template(id,info1,info2,money,max_count)
VALUES (11,'Cùng khám phá','Tiếp tục hành trình cùng bạn bè',0,2147483647)
ON CONFLICT(id) DO UPDATE SET info1=EXCLUDED.info1,info2=EXCLUDED.info2,money=EXCLUDED.money,max_count=EXCLUDED.max_count;
INSERT INTO task_badges_template(id,name,maxcount,idbadgesreward)
VALUES (1,'Cùng luyện tập',2147483647,-1)
ON CONFLICT(id) DO UPDATE SET name=EXCLUDED.name,maxcount=EXCLUDED.maxcount,idbadgesreward=EXCLUDED.idbadgesreward;
CREATE UNIQUE INDEX IF NOT EXISTS super_rank_player_uq ON super_rank(player_id);

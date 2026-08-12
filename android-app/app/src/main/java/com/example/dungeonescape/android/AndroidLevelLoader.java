package com.example.dungeonescape.android;

import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.entity.Entity;
import com.example.dungeonescape.model.entity.Player;
import com.example.dungeonescape.model.goal.Goal;
import com.example.dungeonescape.persistence.EntityDefinition;
import com.example.dungeonescape.persistence.EntityFactory;
import com.example.dungeonescape.persistence.GoalDefinition;
import com.example.dungeonescape.persistence.GoalFactory;
import com.example.dungeonescape.persistence.InvalidLevelException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Android-native JSON loader: no desktop reflection or java.beans dependency. */
final class AndroidLevelLoader {
    private AndroidLevelLoader() {
    }

    static GameState load(InputStream input, String source) {
        try (input) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                bytes.write(buffer, 0, read);
            }
            JSONObject root = new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
            int width = root.getInt("width");
            int height = root.getInt("height");
            if (width < 5 || width > 50 || height < 5 || height > 50) {
                throw new InvalidLevelException(source, "关卡尺寸超出 5..50");
            }
            List<EntityDefinition> definitions = parseEntities(root.getJSONArray("entities"), source);
            validateDefinitions(definitions, width, height, source);
            List<Entity> all = new ArrayList<>();
            for (EntityDefinition definition : definitions) {
                all.add(EntityFactory.create(definition, source));
            }
            Player player = null;
            List<Entity> others = new ArrayList<>();
            for (Entity entity : all) {
                if (entity instanceof Player found) {
                    if (player != null) throw new InvalidLevelException(source, "只能有一个玩家");
                    player = found;
                } else {
                    others.add(entity);
                }
            }
            if (player == null) throw new InvalidLevelException(source, "关卡缺少玩家");
            GoalDefinition goalDefinition = parseGoal(root.getJSONObject("goal"));
            Goal goal = GoalFactory.create(goalDefinition, source, definitions);
            String name = root.optString("name", "Dungeon Escape");
            return new GameState(width, height, player, others, goal, name);
        } catch (IOException | JSONException error) {
            throw new InvalidLevelException(source, "Android JSON 解析失败：" + error.getMessage());
        }
    }

    private static List<EntityDefinition> parseEntities(JSONArray array, String source)
            throws JSONException {
        List<EntityDefinition> result = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject value = array.getJSONObject(i);
            result.add(new EntityDefinition(
                    value.getString("id"),
                    value.getString("type"),
                    value.has("x") ? value.getInt("x") : null,
                    value.has("y") ? value.getInt("y") : null,
                    value.has("keyId") ? value.getInt("keyId") : null,
                    value.has("doorId") ? value.getInt("doorId") : null,
                    value.optBoolean("open", false)));
        }
        return result;
    }

    private static GoalDefinition parseGoal(JSONObject value) throws JSONException {
        List<GoalDefinition> children = null;
        JSONArray array = value.optJSONArray("children");
        if (array != null) {
            children = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                children.add(parseGoal(array.getJSONObject(i)));
            }
        }
        return new GoalDefinition(value.getString("type"), children);
    }

    private static void validateDefinitions(List<EntityDefinition> definitions,
                                            int width, int height, String source) {
        Set<String> ids = new HashSet<>();
        int players = 0;
        for (EntityDefinition definition : definitions) {
            if (!ids.add(definition.id())) {
                throw new InvalidLevelException(source, "实体 ID 重复：" + definition.id());
            }
            if (definition.x() == null || definition.y() == null
                    || definition.x() < 0 || definition.x() >= width
                    || definition.y() < 0 || definition.y() >= height) {
                throw new InvalidLevelException(source, "实体坐标越界：" + definition.id());
            }
            if ("player".equals(definition.type())) players++;
        }
        if (players != 1) throw new InvalidLevelException(source, "必须恰好有一个玩家");
    }
}

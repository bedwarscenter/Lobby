package center.bedwars.lobby.configuration.providers;

import center.bedwars.lobby.configuration.configurations.NametagConfiguration;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.j4c0b3y.api.config.provider.TypeProvider;
import net.j4c0b3y.api.config.provider.context.LoadContext;
import net.j4c0b3y.api.config.provider.context.SaveContext;

import java.util.LinkedHashMap;
import java.util.Map;

public class GroupConfigProvider {

    public static class NametagGroupConfigProvider implements TypeProvider<NametagConfiguration.GroupConfig> {

        @Override
        public NametagConfiguration.GroupConfig load(LoadContext ctx) {
            Object raw = ctx.getObject();

            if (raw instanceof Section map) {
                return new NametagConfiguration.GroupConfig(
                        map.getString("tagprefix", "%phoenix_prefix%"),
                        map.getString("tagsuffix", ""),
                        map.getString("tabprefix", "%phoenix_prefix%"),
                        map.getString("tabsuffix", "")
                );
            }
            return new NametagConfiguration.GroupConfig();
        }

        @Override
        public Object save(SaveContext<NametagConfiguration.GroupConfig> ctx) {
            NametagConfiguration.GroupConfig v = ctx.getObject();

            if (v == null) {
                return new LinkedHashMap<>();
            }

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("tagprefix", v.tagprefix != null ? v.tagprefix : "%phoenix_prefix%");
            out.put("tagsuffix", v.tagsuffix != null ? v.tagsuffix : "");
            out.put("tabprefix", v.tabprefix != null ? v.tabprefix : "%phoenix_prefix%");
            out.put("tabsuffix", v.tabsuffix != null ? v.tabsuffix : "");

            return out;
        }
    }

}

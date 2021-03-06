package net.llamasoftware.spigot.floatingpets.manager.config;

import net.llamasoftware.spigot.floatingpets.FloatingPets;
import net.llamasoftware.spigot.floatingpets.api.model.PetCategory;
import net.llamasoftware.spigot.floatingpets.api.model.Setting;
import net.llamasoftware.spigot.floatingpets.api.model.Skill;
import net.llamasoftware.spigot.floatingpets.model.misc.AnimationType;
import net.llamasoftware.spigot.floatingpets.model.misc.ParticleInfo;
import net.llamasoftware.spigot.floatingpets.model.misc.SkillCategory;
import net.llamasoftware.spigot.floatingpets.model.misc.SkillLevel;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class SettingManager {

    private final FloatingPets plugin;
    private final FileConfiguration config;

    @Getter
    private final List<ParticleInfo> enabledParticles;
    @Getter
    private final List<SkillCategory> skillCategories;
    @Getter
    private final List<PetCategory> categories;
    @Getter
    private final AnimationType animationType;

    public SettingManager(FloatingPets plugin) {
        this.plugin           = plugin;
        this.config           = plugin.getConfig();
        this.enabledParticles = loadEnabledParticles();
        this.skillCategories  = loadSkillCategories();
        this.categories       = loadCategories();
        this.animationType    = loadAnimationType();
    }

    private AnimationType loadAnimationType() {

        if(!plugin.isSetting(Setting.PET_STILL_ANIMATION))
            return AnimationType.NONE;

        String typeStr = plugin.getStringSetting(Setting.PET_STILL_ANIMATION_TYPE);

        if(Arrays.stream(AnimationType.values())
                .noneMatch(type -> type.name().equalsIgnoreCase(typeStr)))
            return AnimationType.NONE;

        return AnimationType.valueOf(typeStr.toUpperCase());

    }

    private List<PetCategory> loadCategories() {

        List<PetCategory> categories = new ArrayList<>();

        addCategoryById(categories, "default");

        if(!config.contains("settings.pet.categories.enabled")
                || !config.getBoolean("settings.pet.categories.enabled"))

            return categories;

        ConfigurationSection section = config.getConfigurationSection("settings.pet.categories.types");
        if(section == null)
            return categories;

        for (String id : section.getKeys(false)) {
            addCategoryById(categories, id);
        }

        return categories;

    }

    private void addCategoryById(List<PetCategory> list, String id){
        ConfigurationSection section = config.getConfigurationSection("settings.pet.categories.types");
        if(section == null)
            return;

        String name = section.getString(id + ".name");
        Material material = Material.getMaterial(Objects.requireNonNull(section.getString(id + ".item")));

        if(material == null)
            return;

        ItemStack stack;
        if(material == Material.PLAYER_HEAD){
            stack = plugin.getNmsHelper().getItemStackFromTexture(section.getString(id + ".texture"));
        } else {
            stack = new ItemStack(material);
        }

        PetCategory category = PetCategory.builder().id(id).name(name).displayItem(stack).build();
        if (list.stream().noneMatch(ca -> ca.getId().equals(category.getId()))) {
            list.add(category);
        }
    }

    private List<ParticleInfo> loadEnabledParticles(){

        ConfigurationSection section = config.getConfigurationSection("settings.pet.particle.display");
        if(section == null)
            return new ArrayList<>();

        String filter = plugin.getStringSetting(Setting.PET_PARTICLE_FILTER);
        List<org.bukkit.Particle> particles;

        if(!filter.equalsIgnoreCase("none")){
            if(!(filter.equalsIgnoreCase("exclude") || filter.equalsIgnoreCase("include"))){
                plugin.getLogger().warning("Invalid particle filter specified in config.");
            }

            if(filter.equalsIgnoreCase("exclude")){
                List<String> excludedParticles = config.getStringList("settings.pet.particle.filter.exclude");
                particles = Arrays.stream(org.bukkit.Particle.values()).filter(particle ->
                        !excludedParticles.contains(particle.name())).collect(Collectors.toList());
            } else {
                List<String> includedParticles = config.getStringList("settings.pet.particle.filter.include");
                particles = Arrays.stream(org.bukkit.Particle.values()).filter(particle ->
                        includedParticles.contains(particle.name())).collect(Collectors.toList());
            }

        } else {
            particles = Arrays.stream(org.bukkit.Particle.values()).collect(Collectors.toList());
        }

        List<ParticleInfo> particleInfos = new ArrayList<>();

        ConfigurationSection displaySection = config.getConfigurationSection("settings.pet.particle.display");
        if(displaySection == null)
            return new ArrayList<>();

        for(org.bukkit.Particle part : particles) {
            String materialStr = displaySection.getString(part.name().toLowerCase());
            particleInfos.add(ParticleInfo.builder().particle(part).material(materialStr == null ?
                    Material.REDSTONE : Material.getMaterial(materialStr)).build());
        }

        return particleInfos;
    }

    private List<SkillCategory> loadSkillCategories(){

        ConfigurationSection section   = config.getConfigurationSection("settings.pet.skills.types");
        List<SkillCategory> categories = new ArrayList<>();

        if(section == null)
            return categories;

        for (String key : section.getKeys(false)) {

            ConfigurationSection skillSection = section.getConfigurationSection(key);
            if(skillSection == null)
                continue;

            Skill.Type type       = Skill.Type.valueOf(key.toUpperCase());
            String displayItemStr = skillSection.getString("display");

            if(displayItemStr == null)
                continue;

            Material material = Material.getMaterial(displayItemStr);
            if(material == null)
                continue;

            ConfigurationSection levelSection = skillSection.getConfigurationSection("levels");
            if(levelSection == null)
                continue;

            LinkedList<SkillLevel> levels = new LinkedList<>();
            for (String levelSectionKey : levelSection.getKeys(false)) {
                int i = Integer.parseInt(levelSectionKey);
                Object value = levelSection.get(levelSectionKey + ".value");
                double cost  = levelSection.getDouble(levelSectionKey + ".cost");
                levels.add(SkillLevel
                        .builder()
                        .type(type)
                        .level(i)
                        .value(value)
                        .cost(cost)
                        .build());
            }

            categories.add(SkillCategory.builder()
                    .type(type)
                    .displayItem(material)
                    .levels(levels)
                    .build());

        }

        return categories;

    }

    public Optional<PetCategory> getCategoryById(String id) {
        return categories.stream()
                .filter(category -> category.getId().equals(id))
                .findAny();
    }

    public Optional<SkillCategory> getCategoryByType(Skill.Type type) {
        return skillCategories.stream()
                .filter(skillCategory -> skillCategory.getType() == type)
                .findAny();
    }

}
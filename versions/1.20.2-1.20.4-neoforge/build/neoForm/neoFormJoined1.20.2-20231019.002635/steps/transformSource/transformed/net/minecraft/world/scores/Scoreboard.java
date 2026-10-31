package net.minecraft.world.scores;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.slf4j.Logger;

public class Scoreboard {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Map<String, Objective> objectivesByName = Maps.newHashMap();
   private final Map<ObjectiveCriteria, List<Objective>> objectivesByCriteria = Maps.newHashMap();
   private final Map<String, Map<Objective, Score>> playerScores = Maps.newHashMap();
   private final Map<DisplaySlot, Objective> displayObjectives = new EnumMap<>(DisplaySlot.class);
   private final Map<String, PlayerTeam> teamsByName = Maps.newHashMap();
   private final Map<String, PlayerTeam> teamsByPlayer = Maps.newHashMap();

   @Nullable
   public Objective getObjective(@Nullable String p_83478_) {
      return this.objectivesByName.get(p_83478_);
   }

   public Objective addObjective(String p_83437_, ObjectiveCriteria p_83438_, Component p_83439_, ObjectiveCriteria.RenderType p_83440_) {
      if (this.objectivesByName.containsKey(p_83437_)) {
         throw new IllegalArgumentException("An objective with the name '" + p_83437_ + "' already exists!");
      } else {
         Objective objective = new Objective(this, p_83437_, p_83438_, p_83439_, p_83440_);
         this.objectivesByCriteria.computeIfAbsent(p_83438_, p_83426_ -> Lists.newArrayList()).add(objective);
         this.objectivesByName.put(p_83437_, objective);
         this.onObjectiveAdded(objective);
         return objective;
      }
   }

   public final void forAllObjectives(ObjectiveCriteria p_83428_, String p_83429_, Consumer<Score> p_83430_) {
      this.objectivesByCriteria
         .getOrDefault(p_83428_, Collections.emptyList())
         .forEach(p_83444_ -> p_83430_.accept(this.getOrCreatePlayerScore(p_83429_, p_83444_)));
   }

   public boolean hasPlayerScore(String p_83462_, Objective p_83463_) {
      Map<Objective, Score> map = this.playerScores.get(p_83462_);
      if (map == null) {
         return false;
      } else {
         Score score = map.get(p_83463_);
         return score != null;
      }
   }

   public Score getOrCreatePlayerScore(String p_83472_, Objective p_83473_) {
      Map<Objective, Score> map = this.playerScores.computeIfAbsent(p_83472_, p_83507_ -> Maps.newHashMap());
      return map.computeIfAbsent(p_83473_, p_83487_ -> {
         Score score = new Score(this, p_83487_, p_83472_);
         score.setScore(0);
         return score;
      });
   }

   public Collection<Score> getPlayerScores(Objective p_83499_) {
      List<Score> list = Lists.newArrayList();

      for(Map<Objective, Score> map : this.playerScores.values()) {
         Score score = map.get(p_83499_);
         if (score != null) {
            list.add(score);
         }
      }

      list.sort(Score.SCORE_COMPARATOR);
      return list;
   }

   public Collection<Objective> getObjectives() {
      return this.objectivesByName.values();
   }

   public Collection<String> getObjectiveNames() {
      return this.objectivesByName.keySet();
   }

   public Collection<String> getTrackedPlayers() {
      return Lists.newArrayList(this.playerScores.keySet());
   }

   public void resetPlayerScore(String p_83480_, @Nullable Objective p_83481_) {
      if (p_83481_ == null) {
         Map<Objective, Score> map = this.playerScores.remove(p_83480_);
         if (map != null) {
            this.onPlayerRemoved(p_83480_);
         }
      } else {
         Map<Objective, Score> map2 = this.playerScores.get(p_83480_);
         if (map2 != null) {
            Score score = map2.remove(p_83481_);
            if (map2.size() < 1) {
               Map<Objective, Score> map1 = this.playerScores.remove(p_83480_);
               if (map1 != null) {
                  this.onPlayerRemoved(p_83480_);
               }
            } else if (score != null) {
               this.onPlayerScoreRemoved(p_83480_, p_83481_);
            }
         }
      }
   }

   public Map<Objective, Score> getPlayerScores(String p_83484_) {
      Map<Objective, Score> map = this.playerScores.get(p_83484_);
      if (map == null) {
         map = Maps.newHashMap();
      }

      return map;
   }

   public void removeObjective(Objective p_83503_) {
      this.objectivesByName.remove(p_83503_.getName());

      for(DisplaySlot displayslot : DisplaySlot.values()) {
         if (this.getDisplayObjective(displayslot) == p_83503_) {
            this.setDisplayObjective(displayslot, null);
         }
      }

      List<Objective> list = this.objectivesByCriteria.get(p_83503_.getCriteria());
      if (list != null) {
         list.remove(p_83503_);
      }

      for(Map<Objective, Score> map : this.playerScores.values()) {
         map.remove(p_83503_);
      }

      this.onObjectiveRemoved(p_83503_);
   }

   public void setDisplayObjective(DisplaySlot p_296205_, @Nullable Objective p_83419_) {
      this.displayObjectives.put(p_296205_, p_83419_);
   }

   @Nullable
   public Objective getDisplayObjective(DisplaySlot p_295165_) {
      return this.displayObjectives.get(p_295165_);
   }

   @Nullable
   public PlayerTeam getPlayerTeam(String p_83490_) {
      return this.teamsByName.get(p_83490_);
   }

   public PlayerTeam addPlayerTeam(String p_83493_) {
      PlayerTeam playerteam = this.getPlayerTeam(p_83493_);
      if (playerteam != null) {
         LOGGER.warn("Requested creation of existing team '{}'", p_83493_);
         return playerteam;
      } else {
         playerteam = new PlayerTeam(this, p_83493_);
         this.teamsByName.put(p_83493_, playerteam);
         this.onTeamAdded(playerteam);
         return playerteam;
      }
   }

   public void removePlayerTeam(PlayerTeam p_83476_) {
      this.teamsByName.remove(p_83476_.getName());

      for(String s : p_83476_.getPlayers()) {
         this.teamsByPlayer.remove(s);
      }

      this.onTeamRemoved(p_83476_);
   }

   public boolean addPlayerToTeam(String p_83434_, PlayerTeam p_83435_) {
      if (this.getPlayersTeam(p_83434_) != null) {
         this.removePlayerFromTeam(p_83434_);
      }

      this.teamsByPlayer.put(p_83434_, p_83435_);
      return p_83435_.getPlayers().add(p_83434_);
   }

   public boolean removePlayerFromTeam(String p_83496_) {
      PlayerTeam playerteam = this.getPlayersTeam(p_83496_);
      if (playerteam != null) {
         this.removePlayerFromTeam(p_83496_, playerteam);
         return true;
      } else {
         return false;
      }
   }

   public void removePlayerFromTeam(String p_83464_, PlayerTeam p_83465_) {
      if (this.getPlayersTeam(p_83464_) != p_83465_) {
         throw new IllegalStateException("Player is either on another team or not on any team. Cannot remove from team '" + p_83465_.getName() + "'.");
      } else {
         this.teamsByPlayer.remove(p_83464_);
         p_83465_.getPlayers().remove(p_83464_);
      }
   }

   public Collection<String> getTeamNames() {
      return this.teamsByName.keySet();
   }

   public Collection<PlayerTeam> getPlayerTeams() {
      return this.teamsByName.values();
   }

   @Nullable
   public PlayerTeam getPlayersTeam(String p_83501_) {
      return this.teamsByPlayer.get(p_83501_);
   }

   public void onObjectiveAdded(Objective p_83422_) {
   }

   public void onObjectiveChanged(Objective p_83455_) {
   }

   public void onObjectiveRemoved(Objective p_83467_) {
   }

   public void onScoreChanged(Score p_83424_) {
   }

   public void onPlayerRemoved(String p_83431_) {
   }

   public void onPlayerScoreRemoved(String p_83432_, Objective p_83433_) {
   }

   public void onTeamAdded(PlayerTeam p_83423_) {
   }

   public void onTeamChanged(PlayerTeam p_83456_) {
   }

   public void onTeamRemoved(PlayerTeam p_83468_) {
   }

   public void entityRemoved(Entity p_83421_) {
      if (!(p_83421_ instanceof Player) && !p_83421_.isAlive()) {
         String s = p_83421_.getStringUUID();
         this.resetPlayerScore(s, null);
         this.removePlayerFromTeam(s);
      }
   }

   protected ListTag savePlayerScores() {
      ListTag listtag = new ListTag();
      this.playerScores.values().stream().map(Map::values).forEach(p_294093_ -> p_294093_.forEach(p_166096_ -> {
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putString("Name", p_166096_.getOwner());
            compoundtag.putString("Objective", p_166096_.getObjective().getName());
            compoundtag.putInt("Score", p_166096_.getScore());
            compoundtag.putBoolean("Locked", p_166096_.isLocked());
            listtag.add(compoundtag);
         }));
      return listtag;
   }

   protected void loadPlayerScores(ListTag p_83446_) {
      for(int i = 0; i < p_83446_.size(); ++i) {
         CompoundTag compoundtag = p_83446_.getCompound(i);
         String s = compoundtag.getString("Name");
         String s1 = compoundtag.getString("Objective");
         Objective objective = this.getObjective(s1);
         if (objective == null) {
            LOGGER.error("Unknown objective {} for name {}, ignoring", s1, s);
         } else {
            Score score = this.getOrCreatePlayerScore(s, objective);
            score.setScore(compoundtag.getInt("Score"));
            if (compoundtag.contains("Locked")) {
               score.setLocked(compoundtag.getBoolean("Locked"));
            }
         }
      }
   }
}

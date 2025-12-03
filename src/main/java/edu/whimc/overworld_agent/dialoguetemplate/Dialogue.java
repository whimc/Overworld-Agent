package edu.whimc.overworld_agent.dialoguetemplate;




import edu.whimc.overworld_agent.OverworldAgent;
import edu.whimc.overworld_agent.dialoguetemplate.models.Chatbot;
import edu.whimc.overworld_agent.dialoguetemplate.models.DialoguePrompt;

import edu.whimc.overworld_agent.utils.Utils;
import edu.whimc.sciencetools.models.sciencetool.ScienceTool;
import edu.whimc.sciencetools.models.sciencetool.ScienceToolMeasureEvent;
//import me.blackvein.quests.Objective;
//import me.blackvein.quests.Quest;
//import me.blackvein.quests.Quests;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.FollowTrait;
import net.citizensnpcs.trait.SkinTrait;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.bukkit.search.event.BukkitFoundSolutionEvent;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.bukkit.util.BukkitUtil;
import net.whimxiqal.journey.navigation.Itinerary;
import net.whimxiqal.journey.navigation.Step;
import net.whimxiqal.journey.search.event.FoundSolutionEvent;
import org.apache.commons.lang.StringUtils;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.Consumer;


public class Dialogue implements Listener {
    private SpigotCallback spigotCallback;
    /* Unicode for bullet character */
    private static final String BULLET = "\u2022";
    private OverworldAgent plugin;
    private Player player;
    private final int PROFANITY_LABEL = -1;
    private final int UNKNOWN_LABEL = -2;
    private final double THRESHOLD = .5;
    private final int AGENT_EDIT_NUM = 5;
    private String feedback;
    private String response;
    private boolean text;
    private boolean embodied;
    private Map<Integer, DialoguePrompt> prompts;
    public Dialogue(OverworldAgent plugin, Player player, boolean text, boolean embodied) {
        this.spigotCallback = new SpigotCallback(plugin);
        this.plugin = plugin;
        this.player = player;
        feedback = "";
        //Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        response = "";
        this.text = text;
        this.embodied = embodied;
        prompts = new HashMap<>();

        String path = "prompts";
        List<Map<?, ?>> entries = plugin.getConfig().getMapList(path);
        for (Map<?, ?> entry : entries) {
            int label =  Integer.parseInt(String.valueOf(entry.get("label")));
            this.prompts.put(label,new DialoguePrompt(entry));
        }
    }

    public void doDialogue() {
        Utils.msgNoPrefix(player, "&lWhat do you want to discuss?", "");
        String endResponse = plugin.getConfig().getString("template-gui.text.end-your-own-response-speech");
        String customResponse = plugin.getConfig().getString("template-gui.text.write-your-own-response");
        String seeDialogue = plugin.getConfig().getString("template-gui.text.see-all-responses");
        String signHeader = plugin.getConfig().getString("template-gui.text.custom-response-sign-header");
        String guidanceResponse = plugin.getConfig().getString("template-gui.text.guidance-response");
        String showResponse = plugin.getConfig().getString("template-gui.text.show-response");
        String tagScoreResponse = plugin.getConfig().getString("template-gui.text.tag-score-response");
        String scoreResponse = plugin.getConfig().getString("template-gui.text.score-response");
        String agentEdit = plugin.getConfig().getString("template-gui.text.agent-edit");
        Map<String, Cell> waypoints = Journey.get().dataManager().publicWaypointManager().getAll();
        List<String> locationOnWorld = new ArrayList<>();
        for (Map.Entry<String, Cell> entry : waypoints.entrySet()) {
            if (BukkitUtil.getWorld(entry.getValue()).getName().equals(player.getWorld().getName())) {
                locationOnWorld.add(entry.getKey());
            }
        }
        //Agent Guidance Option
        if(locationOnWorld.size() > 0) {
            sendComponent(
                    player,
                    "&8" + BULLET + guidanceResponse,
                    "&aClick here to let me show you something cool!",
                    p -> {
                        this.spigotCallback.clearCallbacks(player);
                        Utils.msgNoPrefix(player, "&lClick the location you want to go to:", "");

                        for (String entry : locationOnWorld) {
                                sendComponent(
                                        player,
                                        "&8" + BULLET + " &r" + entry,
                                        "&aClick here to select \"&r" + entry + "&a\"",
                                        l -> {
                                            this.plugin.getQueryer().storeNewInteraction(new Interaction(plugin, player, "Guidance"), id -> {
                                                String location = entry;
                                                if (location.contains(" ")) {
                                                    location = "\"" + location + "\"";
                                                }
                                                Bukkit.dispatchCommand(player, "jt " + location);
                                                this.spigotCallback.clearCallbacks(player);
                                            });
                                        });
                        }

                    });
        }
        //Agent Tag option
        Map<Player, Map<World, Integer>> playerTags = Tag.getPlayerTags();
        int numTags = 0;
        if(playerTags.get(player) != null && playerTags.get(player).get(player.getWorld()) != null){
            numTags = playerTags.get(player).get(player.getWorld());
        }
        if (Tag.maxTags(player.getWorld()) != null && numTags < Tag.maxTags(player.getWorld()) && Tag.getDialogueTags().get(player.getWorld()) != null) {
            sendComponent(
                    player,
                    "&8" + BULLET + showResponse,
                    "&aClick here to show me/ask about something unique to this planet!",
                    p -> this.plugin.getSignMenuFactory()
                            .newMenu(Collections.singletonList(Utils.color(signHeader)))
                            .reopenIfFail(true)
                            .response((signPlayer, strings) -> {
                                String response = StringUtils.join(Arrays.copyOfRange(strings, 0, strings.length), ' ').trim();
                                response = response.toLowerCase();
                                if (response.isEmpty()) {
                                    return false;
                                }
                                String finalResponse = response;
                                this.plugin.getQueryer().storeNewInteraction(new Interaction(plugin, player, "Tag"), id -> {
                                    Tag tag = new Tag(plugin, player, finalResponse);
                                    tag.sendFeedback();
                                    this.spigotCallback.clearCallbacks(player);
                                });
                                return true;
                            })
                            .open(p)
            );
        }
/**
        //Agent Score option
        sendComponent(
                player,
                "&8" + BULLET + scoreResponse,
                "&aClick here to see your scores!",
                p -> {

                    this.plugin.getQueryer().storeNewInteraction(new Interaction(plugin, player, "Progress"), id -> {
                        Bukkit.dispatchCommand(player,  "progress");
                    });
                });

        //Agent Dialogue option
        if (text) {
            sendComponent(
                    player,
                    "&8" + BULLET + customResponse,
                    "&aClick here to write your own response!",
                    p -> this.plugin.getSignMenuFactory()
                            .newMenu(Collections.singletonList(Utils.color(signHeader)))
                            .reopenIfFail(true)
                            .response((signPlayer, strings) -> {
                                response = StringUtils.join(Arrays.copyOfRange(strings, 0, strings.length), ' ').trim();
                                response = response.toLowerCase();

                                if (response.isEmpty()) {
                                    return false;
                                }
                                doResponse();
                                return true;
                            })
                            .open(p)
            );
        } else {
            sendComponent(
                    player,
                    "&8" + BULLET + endResponse,
                    "&aClick here to see my response!",
                    p -> {
                        player.sendMessage(response);
                        doResponse();
                    });
        }

        //Agent Reflection Option
        sendComponent(
                player,
                "&8" + BULLET + seeDialogue,
                "&aClick here to see our conversation so far!",
                p -> {
                    plugin.getQueryer().getSessionConversation(player, plugin.getPlayerSessions().get(player), conversation -> {
                        HashMap<String, List<String>> dialogue = (HashMap<String, List<String>>) conversation;
                        for (Map.Entry<String, List<String>> entry : dialogue.entrySet()) {
                            String world = entry.getKey();
                            List<String> discussion = entry.getValue();
                            for (int k = 0; k < discussion.size(); k++) {
                                if (k % 2 == 0) {
                                    player.sendMessage(world + ": " + player.getName() + ": " + discussion.get(k));
                                } else {
                                    player.sendMessage(world + ": " + plugin.getAgents().get(player.getName()).getName() + ": " + discussion.get(k));
                                }
                            }
                        }
                    });
                    this.plugin.getQueryer().storeNewInteraction(new Interaction(plugin, player, "Reflection"), id -> {

                    });
                });
*/
        int skinChange = plugin.getAgentEdits().get(player).get("Skin");
        int nameChange = plugin.getAgentEdits().get(player).get("Name");
        if((skinChange < AGENT_EDIT_NUM || nameChange < AGENT_EDIT_NUM) && embodied){
        //Agent edit Option
        sendComponent(player, "&8" + BULLET + agentEdit, "&aClick here to change me!", p -> {
            this.spigotCallback.clearCallbacks(player);
            Utils.msgNoPrefix(player, "&lClick what you want to change:", "");

            if(skinChange < AGENT_EDIT_NUM) {
                sendComponent(
                        player,
                        "&8" + BULLET + " &rSkin",
                        "&aClick here to select \"&rskin change",
                        l -> {
                            Utils.msgNoPrefix(player, "&lClick what skin you want me to have:", "");
                            FileConfiguration config = plugin.getConfig();
                            String path = "skins."+plugin.getSkinType();
                            for (String key : config.getConfigurationSection(path).getKeys(false)) {
                                ConfigurationSection section = config.getConfigurationSection(path + "." + key);
                                String label = section.getString("dialogue_option");
                                String signature = section.getString("signature");
                                String data = section.getString("data");
                                String skinName = key;
                                sendComponent(
                                        player,
                                        "&8" + BULLET + " &r" + label,
                                        "&aClick here to select \"&r" + label + "&a\"",
                                        m -> {
                                            this.plugin.getQueryer().storeNewInteraction(new Interaction(plugin, player, "Edit"), id -> {
                                                Map<String, NPC> npcs = plugin.getAgents();
                                                NPC npc = npcs.get(player.getName());
                                                if (npc != null) {
                                                    SkinTrait skinTrait = npc.getOrAddTrait(SkinTrait.class);
                                                    skinTrait.setSkinPersistent(skinName, signature, data);
                                                    plugin.getAgentEdits().get(player).replace("Skin",skinChange+1);
                                                    int numLeft = AGENT_EDIT_NUM - plugin.getAgentEdits().get(player).get("Skin");
                                                    player.sendMessage("Your agent's skin has been changed to " + label + ".\n You have " + numLeft + " skin edits left.");
                                                    plugin.getQueryer().storeNewAgent(player, "edit", npc.getName(), skinName, id2 -> {
                                                        plugin.getAgents().put(player.getName(), npc);
                                                    });
                                                } else {
                                                    player.sendMessage("You need to have an AI friend first. Please try again");
                                                }
                                                this.spigotCallback.clearCallbacks(player);
                                            });
                                        });
                            }
                        });
            }
            if(nameChange < AGENT_EDIT_NUM){
            sendComponent(
                    player,
                    "&8" + BULLET + " &rName",
                    "&aClick here to select \"&rname change",
                    l -> this.plugin.getSignMenuFactory()
                            .newMenu(Collections.singletonList(Utils.color(signHeader)))
                            .reopenIfFail(true)
                            .response((signPlayer, strings) -> {
                                String agentName = StringUtils.join(Arrays.copyOfRange(strings, 0, strings.length), ' ').trim();

                                if (agentName.isEmpty()) {
                                    return false;
                                } else if (agentName.length() > 25){
                                    agentName = agentName.substring(0,25);
                                }
                                Map<String, NPC> npcs = plugin.getAgents();
                                NPC npc = npcs.get(player.getName());

                                String finalAgentName = agentName;
                                this.plugin.getQueryer().storeNewInteraction(new Interaction(plugin, player, "Edit"), id -> {
                                    if (npc != null) {
                                        npc.setName(finalAgentName);
                                        plugin.getAgentEdits().get(player).replace("Name",nameChange+1);
                                        int numLeft = AGENT_EDIT_NUM - plugin.getAgentEdits().get(player).get("Name");
                                        player.sendMessage("Your agent's name has been changed to " + finalAgentName + ".\n You have " + numLeft + " name edits left.");
                                        plugin.getQueryer().storeNewAgent(player, "edit", finalAgentName, npc.getOrAddTrait(SkinTrait.class).getSkinName(), id2 -> {
                                            plugin.getAgents().put(player.getName(), npc);
                                        });
                                    } else {
                                        player.sendMessage("You need to have an AI friend first. Please try again");
                                    }
                                    this.spigotCallback.clearCallbacks(player);
                                });
                                return true;
                            })
                            .open(p)
            );}
        });}
    }

/**
    private void doResponse() {
        DialoguePrompt prompt = null;

            Chatbot chatbot = new Chatbot(response);
            double[] prediction = chatbot.predict();
            int predictedClass = (int) prediction[0];
            double certainty = prediction[1];
            if (certainty > THRESHOLD) {
                prompt = prompts.get(predictedClass);
                feedback = prompt.getFeedback();
                this.fillIn();
                if (prompt.getPrompt().equalsIgnoreCase("quest")) {
                    int ctr = 0;
                    Quests qp = (Quests) Bukkit.getServer().getPluginManager().getPlugin("Quests");
                    for (Quest quest : qp.getQuester(player.getUniqueId()).getCurrentQuests().keySet()) {
                        feedback += quest.getDescription();
                        ctr++;
                    }
                    if (ctr == 0) {
                        feedback = "You are not on any quest currently";
                    }

                } else if (prompt.getPrompt().equalsIgnoreCase("guidance")) {
                    DialoguePrompt finalPrompt = prompt;
                    String[] split = response.replaceAll("[^a-zA-Z]", "").toLowerCase().split("\\s+");
                    String destination = "";
                    Map<String, Cell> waypoints = Journey.get().dataManager().publicWaypointManager().getAll();
                    for (Map.Entry<String,Cell> entry : waypoints.entrySet()) {
                        for (String word : split) {
                            word = word.toLowerCase();
                            String locationLower = entry.getKey().toLowerCase();
                            if (locationLower.contains(word)) {
                                destination = entry.getKey();
                                break;
                            }
                        }
                    }
                    feedback = feedback.replace("{LOCATION}", destination);
                    if(destination.equals("")){
                        feedback = "Sorry, I could not find that location";
                    } else {
                        String finalDestination = destination;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            Bukkit.dispatchCommand(player, finalPrompt.getTool() + " " + finalDestination);
                        });
                    }
                } else if (prompt.getPrompt().equalsIgnoreCase("npcs")) {
                    int ctr = 0;
                    Iterable<NPC> serverNPCs = CitizensAPI.getNPCRegistry().sorted();
                    for (NPC currNPC : serverNPCs) {
                        if ((currNPC.getStoredLocation() != null) && (currNPC.isSpawned()) && (currNPC.getStoredLocation().getWorld().equals(player.getWorld())) && (!plugin.getAgents().containsValue(currNPC))) {
                            feedback += currNPC.getName() + "'s location is (" + currNPC.getStoredLocation().getBlockX() + ", " + currNPC.getStoredLocation().getBlockY() + ", " + currNPC.getStoredLocation().getBlockZ() + ")\n";
                            ctr++;
                        }
                    }
                    if (ctr == 0) {
                        feedback = "There are currently no characters on your world";
                    }

                } else if (prompt.getPrompt().equalsIgnoreCase("science_tool")) {
                    DialoguePrompt finalPrompt = prompt;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        Bukkit.dispatchCommand(player, finalPrompt.getTool());
                    });
                }
            } else {
                prompt = prompts.get(UNKNOWN_LABEL);
                feedback = prompt.getFeedback();

            }
        //}
        String finalResponse = response;
        //Janky but waits until event is done before stores in db
        DialoguePrompt finalPrompt1 = prompt;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            this.plugin.getQueryer().storeNewScienceInquiry(player, finalResponse, feedback, id -> {
                this.plugin.getQueryer().storeNewInteraction(new Interaction(plugin, player, "Dialogue"), id2 -> {
                    this.spigotCallback.clearCallbacks(player);
                    if (!finalPrompt1.getPrompt().equalsIgnoreCase("science_tool")) {
                        player.sendMessage(feedback);
                    }
                });
            });
        }, 20L);

    }
*/
    private void sendComponent(Player player, String text, String hoverText, Consumer<Player> onClick) {
        player.spigot().sendMessage(createComponent(text, hoverText, onClick));
    }

    private TextComponent createComponent(String text, String hoverText, Consumer<Player> onClick) {
        TextComponent message = new TextComponent(Utils.color(text));
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Utils.color(hoverText)).create()));
        addCallback(message, this.player.getUniqueId(), onClick);
        return message;
    }

    private void addCallback(TextComponent component, UUID playerUUID, Consumer<Player> onClick) {
        this.spigotCallback.createCommand(playerUUID, component, onClick);
    }
/**
    public void fillIn() {
        feedback = feedback.replace("{NAME}", player.getName());
        feedback = feedback.replace("{PLANET}", player.getWorld().getName());
        if (plugin.getAgents().get(player.getName()) != null) {
            feedback = feedback.replace("{AGENT}", plugin.getAgents().get(player.getName()).getName());
        }
    }

    @EventHandler
    public void onToolUse(ScienceToolMeasureEvent measure) {
        Player eventPlayer = measure.getMeasurement().getPlayer();
        if (this.player.equals(eventPlayer)  && plugin.getAgents().get(player.getName()) != null) {
            ScienceTool tool = measure.getMeasurement().getTool();
            feedback = feedback.replace("{TOOL}", tool.getDisplayName());
            feedback = feedback.replace("{MEASUREMENT}", measure.getMeasurement().getMeasurement());
        }
    }


    @EventHandler
    public void onVoice(VoiceEvent e) {
        Player p = e.getPlayer();
        if(p.equals(player) && !text) {
            player.sendMessage(e.getSentence());
            response = e.getSentence();
        }
    }

    @EventHandler
    public void walkPath(BukkitFoundSolutionEvent path) {
        FoundSolutionEvent event = path.getSearchEvent();
        Player eventPlayer = Bukkit.getPlayer(event.getSession().getCallerId());
        if (this.player.equals(eventPlayer) && plugin.getAgents().get(player.getName()) != null) {
            NPC agent = plugin.getAgents().get(player.getName());
            if (agent.isSpawned()) {
                //player.sendMessage("Make sure to look around while we walk! If you want to check out other stuff, the path will still be here until later.");
                if(agent.getOrAddTrait(FollowTrait.class).isActive()) {
                    agent.getOrAddTrait(FollowTrait.class).toggle(player, false);
                }

                Itinerary itinerary = event.getItinerary();
                ArrayList<Step> steps = itinerary.getSteps();
                final int[] step = {Math.min(0, steps.size() - 1)};
                final int[] goal = {Math.min(step[0]+5, steps.size() - 1)};
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (goal[0] >= steps.size()-1) {
                            //player.sendMessage("Thanks for following me, try making an observation here about our surroundings!");
                            if(!agent.getOrAddTrait(FollowTrait.class).isActive()) {
                                agent.getOrAddTrait(FollowTrait.class).toggle(player, false);
                            }
                            cancel();
                        }
                        Cell cell = steps.get(goal[0]).location();
                        Location target = new Location(player.getWorld(), cell.blockX(), cell.blockY(), cell.blockZ());
                        agent.getNavigator().setTarget(target);
                        if(agent.getStoredLocation().distanceSquared(target) <= 9){
                            step[0] = goal[0];
                            goal[0] = Math.min(step[0] + 5, steps.size() - 1);
                        }

                        if (agent.getStoredLocation().distance(player.getLocation()) > 10) {
                            //player.sendMessage("Let's explore other areas of the map. This path will stay here and we can return to it later.");
                            if(!agent.getOrAddTrait(FollowTrait.class).isActive()) {
                                agent.getOrAddTrait(FollowTrait.class).toggle(player, false);
                            }
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin,0,0);
            }
        }
    }
    */

}


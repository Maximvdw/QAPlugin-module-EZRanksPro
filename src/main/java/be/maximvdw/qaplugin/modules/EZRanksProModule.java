package be.maximvdw.qaplugin.modules;

import be.maximvdw.qaplugin.api.AIModule;
import be.maximvdw.qaplugin.api.AIQuestionEvent;
import be.maximvdw.qaplugin.api.QAPluginAPI;
import be.maximvdw.qaplugin.api.ai.*;
import be.maximvdw.qaplugin.api.exceptions.FeatureNotEnabled;
import me.clip.ezrankspro.EZAPI;
import me.clip.ezrankspro.EZRanksPro;
import me.clip.ezrankspro.multipliers.CostHandler;
import me.clip.ezrankspro.rankdata.LastRank;
import me.clip.ezrankspro.rankdata.Rankup;
import me.clip.ezrankspro.util.EcoUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * EZRanksProModule
 * <p>
 * Adds AI to the assistant to ask for rank information.
 * <p>
 * Created by maxim on 27-Dec-16.
 */
public class EZRanksProModule extends AIModule {
    public EZRanksProModule() {
        super("ezrankspro", "Maximvdw", "Ask for rank information");

        // DEBUG
        boolean forceUpdate = true;

        // Entity with rank names
        Entity rankupEntity = new Entity("ezrankspro-ranks");
        List<Rankup> rankups = getAllRankups();
        for (Rankup rank : rankups) {
            rankupEntity.addEntry(new EntityEntry(rank.getRank()));
        }


        // Question to ask for the current rank
        Intent qCurrentRank = new Intent("QAPlugin-module-ezrankspro-rank.current")
                // These are the possible questions
                // Note that the API is smart to detect
                // variations on these templates
                .addTemplate("what rank do I have?")
                .addTemplate("what is my current rank?")
                .addTemplate("can you tell me my rank?")
                .addTemplate("what is the name of my rank?")
                .addTemplate("on what rank am I?")
                .addResponse(new IntentResponse()
                        // The response will trigger this action
                        .withAction(this)
                        // This is the output context with a lifetime of 1 question
                        // this context contains the variable data (for this question nothing)
                        .addAffectedContext(new Context("ezrankspro", 1))
                        // I use a 'question' parameter as the way to identify questions
                        // preferably this would be done using the 'ACTION name'
                        // but this requires more classes
                        .addParameter(new IntentResponse.ResponseParameter("question", QuestionType.RANK_CURRENT.name()))
                        .addMessage(new IntentResponse.TextResponse()
                                // These are possible speech responses
                                // I added them here so the user can configure them online
                                // Notice that other questions do not have this section configured
                                // because some questions need completly different answers
                                // depending on the answer (true/false for example)
                                // Keep in mind that you do have to replace the $rank placeholder
                                // yourself
                                .addSpeechText("You are currently on rank $rank!")
                                .addSpeechText("Your current rank is $rank")
                                .addSpeechText("This is your current rank: $rank")));
        // These are the error responses
        // I like to put them close to the question
        // The first param is just a name you give to it
        addErrorResponse("rank_current-no-rank", "You are not in any rank!");
        addErrorResponse("rank_current-no-rank", "I couldn't find your rank!");
        addErrorResponse("rank_current-no-rank", "You do not seem to have a rank?");


        // Question to ask if you are on the last rank
        Intent qLastRankMe = new Intent("QAPlugin-module-ezrankspro-rank.islast.me")
                .addTemplate("am I on the last rank?")
                .addTemplate("can I still rank up?")
                .addTemplate("can I still rankup?")
                .addResponse(new IntentResponse()
                        .withAction(this)
                        .addAffectedContext(new Context("ezrankspro", 1))
                        .addParameter(new IntentResponse.ResponseParameter("question", QuestionType.IS_LAST_ME.name())));
        // These the possible answers
        // The first param is just a name you give to it
        addResponse("is_last_me-true", "You are on the last rank indeed ;) Good job!");
        addResponse("is_last_me-true", "Yes, you are on the last rank");
        addResponse("is_last_me-true", "Yes");
        addResponse("is_last_me-true", "You are on the last rank");
        addResponse("is_last_me-true", "You are indeed on the last rank!");
        addResponse("is_last_me-false", "No, sorry");
        addResponse("is_last_me-false", "No.. you are not on the last rank");
        addResponse("is_last_me-false", "You can still rank up!");


        // Question to ask for the last rank
        Intent qLastRank = new Intent("QAPlugin-module-ezrankspro-rank.last")
                .addTemplate("what is the last rank?")
                .addTemplate("what is the maximum rank?")
                .addTemplate("what is the last rank I can get?")
                .addResponse(new IntentResponse()
                        .withAction(this)
                        .addAffectedContext(new Context("rank", 1))
                        .addParameter(new IntentResponse.ResponseParameter("question", QuestionType.RANK_LAST.name()))
                        .addMessage(new IntentResponse.TextResponse()
                                .addSpeechText("The last rank is $lastrank")
                                .addSpeechText("The maximum rank is $lastrank?")));


        // Question to ask for the next rank
        Intent qCurrentRankup = new Intent("QAPlugin-module-ezrankspro-rank.next")
                .addTemplate("what is the next rank?")
                .addTemplate("what is my current rankup?")
                .addTemplate("what is my next rank?")
                .addTemplate("when rank will I have next?")
                .addResponse(new IntentResponse()
                        .withAction(this)
                        .addAffectedContext(new Context("rank", 1))
                        .addParameter(new IntentResponse.ResponseParameter("question", QuestionType.RANK_NEXT.name())));


        // Question to ask how much it costs for a next rank
        Intent qRankupCost = new Intent("QAPlugin-module-ezrankspro-rank.next.cost")
                .addTemplate("how much does it cost to rankup?")
                .addTemplate("how much does my next rank cost?")
                .addTemplate("how much does it totally cost to rankup?")
                .addResponse(new IntentResponse()
                        .withAction(this)
                        .addAffectedContext(new Context("rank", 1))
                        .addParameter(new IntentResponse.ResponseParameter("question", QuestionType.RANK_NEXT_COST.name())));
        addErrorResponse("rank_next_cost-last", "You can't rank up!");
        addErrorResponse("rank_next_cost-last", "You can no longer rankup.");
        addErrorResponse("rank_next_cost-last", "You have reached the maximum rank.");


        // Question to ask how much money you need for the next rank
        Intent qRankupRemaining = new Intent("QAPlugin-module-ezrankspro-rank.next.remaining")
                .addTemplate("how much do I need to rankup?")
                .addTemplate("how much money do I need to rankup?")
                .addTemplate("how much money do I still need for my next rank?")
                .addResponse(new IntentResponse()
                        .withAction(this)
                        .addAffectedContext(new Context("rank", 1))
                        .addParameter(new IntentResponse.ResponseParameter("question", QuestionType.RANK_NEXT_REMAINING.name()))
                        .addMessage(new IntentResponse.TextResponse()
                                .addSpeechText("You still need $money for the next rank!")
                                .addSpeechText("You still have to get $money before you can rank up")
                                .addSpeechText("You need $money")));
        addErrorResponse("rank_next_remaining-last", "You can't rank up!");
        addErrorResponse("rank_next_remaining-last", "You can no longer rankup.");
        addErrorResponse("rank_next_remaining-last", "You have reached the maximum rank.");

        // Question to ask the rank prefix
        Intent qRankPrefix = new Intent("QAPlugin-module-ezrankspro-rank.prefix")
                .addTemplate("what is my rank prefix?")
                .addTemplate("what prefix does my rank have?")
                .addTemplate("what is the prefix of my rank?")
                .addResponse(new IntentResponse()
                        .withAction(this)
                        .addAffectedContext(new Context("rank", 1))
                        .addParameter(new IntentResponse.ResponseParameter("question", QuestionType.RANK_PREFIX.name()))
                        .addMessage(new IntentResponse.TextResponse()
                                .addSpeechText("The prefix of your rank is $prefix")
                                .addSpeechText("Your rank uses $prefix as the prefix")
                                .addSpeechText("$prefix is your prefix!")));

        // Question to ask how much your current rank had cost
        Intent qCurrentRankCost = new Intent("QAPlugin-module-ezrankspro-rank.cost")
                .addTemplate("how much did my current rank cost?")
                .addTemplate("how much did the rank I currently have cost?")
                .addTemplate("how much did I pay for the current rank?")
                .addResponse(new IntentResponse()
                        .withAction(this)
                        .addAffectedContext(new Context("rank", 1))
                        .addParameter(new IntentResponse.ResponseParameter("question", QuestionType.RANK_COST.name()))
                        .addMessage(new IntentResponse.TextResponse()
                                .addSpeechText("Your current rank's price was $money")
                                .addSpeechText("The price for your current rank was $money")
                                .addSpeechText("You paid $money for your current rank")));
        addErrorResponse("rank_cost-no-rank", "You are not in any rank!");
        addErrorResponse("rank_cost-no-rank", "I couldn't find your rank!");
        addErrorResponse("rank_cost-no-rank", "You do not seem to have a rank?");

        // Question to ask the rank of another player
        Intent qRankOtherPlayer = new Intent("QAPlugin-module-ezrankspro-rank.otherplayer")
                .addTemplate("what rank does he have?")
                .addTemplate(new IntentTemplate()
                        .addPart("what rank does ")
                        .addPart(new IntentTemplate.TemplatePart("Maximvdw")
                                .withMeta("@sys.any")
                                .withAlias("player"))
                        .addPart(" have?"))
                .addTemplate(new IntentTemplate()
                        .addPart("what is the rank of ")
                        .addPart(new IntentTemplate.TemplatePart("AppleFan")
                                .withMeta("@sys.any")
                                .withAlias("player"))
                        .addPart(" ?"))
                .addTemplate(new IntentTemplate()
                        .addPart("what's the rank of ")
                        .addPart(new IntentTemplate.TemplatePart("DiamondBoy123")
                                .withMeta("@sys.any")
                                .withAlias("player"))
                        .addPart(" ?"))
                .addTemplate(new IntentTemplate()
                        .addPart("on what rank is ")
                        .addPart(new IntentTemplate.TemplatePart("Maximvdw")
                                .withMeta("@sys.any")
                                .withAlias("player"))
                        .addPart(" currently?"))
                .addResponse(new IntentResponse()
                        .withAction(this)
                        // I am adding rank_other as a context so I can ask questions
                        // such as "how much did he pay for it?" referring to the player that
                        // is given here
                        .addAffectedContext(new Context("rank_other", 1))
                        .addParameter(new IntentResponse.ResponseParameter("question", QuestionType.RANK_OTHERPLAYER.name()))
                        // Adding the player name as a parameter
                        .addParameter(new IntentResponse.ResponseParameter("player", "$player")
                                // I am setting the default value to the CONTEXT parameter "player" of the context "rank_other"
                                // This will allow you to ask "what was the rank he had again?"
                                // because it will fill it in with this default value
                                .withDefaultValue("#rank_other.player")
                                // Try not to use sys.any
                                .withDataType("@sys.any")
                                .setRequired(true)
                                .addPrompt("For what player do you want to know the rank?")
                                .addPrompt("What is the name of the player you want to know the rank of?")
                                .addPrompt("Can you tell me the name of the player you want to know the rank of?")
                                .addPrompt("What is the name of the player you want to know the rank of?"))
                        .addMessage(new IntentResponse.TextResponse()
                                .addSpeechText("$player is currently on rank $rank!")
                                .addSpeechText("That player is currently on rank $rank")
                                .addSpeechText("He is currently on rank $rank")));
        addErrorResponse("rank_otherplayer-no-player","That does not seem to be a player?");
        addErrorResponse("rank_otherplayer-no-player","That does not seem to be a real player?");
        addErrorResponse("rank_otherplayer-no-player","Are you sure that is a real player?");
        addErrorResponse("rank_otherplayer-no-player","Are you sure that player exists?");
        addErrorResponse("rank_otherplayer-no-player","I can't seem to find that player...");
        addErrorResponse("rank_otherplayer-no-player","I don't think that is a real player");
        addErrorResponse("rank_otherplayer-not-online","I can only get the rank of online players, sorry :(");
        addErrorResponse("rank_otherplayer-not-online","I can only get the rank of online players");
        addErrorResponse("rank_otherplayer-not-online","I can not get the rank of the player");
        addErrorResponse("rank_otherplayer-not-online","I can not get that player's rank");
        addErrorResponse("rank_otherplayer-not-online","Is that player online?");
        addErrorResponse("rank_otherplayer-not-online","I can only get the rank of an online player");

        // Question to ask for the rank cost of another player
        Intent qRankOtherPlayerCost = new Intent("QAPlugin-module-ezrankspro-rank.otherplayer.cost")
                .addTemplate("how much did it cost?")
                .addTemplate("how much did he pay for it?")
                .addTemplate("how much money did he need for it?")
                .addTemplate("how much money did he need for that rank?")
                .addTemplate("how much did that rank cost?")
                .addResponse(new IntentResponse()
                        .withAction(this)
                        .addAffectedContext(new Context("rank_other", 1))
                        .addParameter(new IntentResponse.ResponseParameter("question", QuestionType.RANK_OTHERPLAYER_COST.name()))
                        .addMessage(new IntentResponse.TextResponse()
                                .addSpeechText("He paid $money for it")
                                .addSpeechText("That player is currently on rank $rank")
                                .addSpeechText("He is currently on rank $rank")));

        try {
            // Upload the entities
            if (!QAPluginAPI.uploadEntity(rankupEntity)) {
                warning("Unable to upload entity!");
            }

            // Upload the intents
            // I check if it already exists so I don't have to upload
            // it on every reload. I don't do this for entities because they
            // can change.
            if (QAPluginAPI.findIntentByName(qCurrentRank.getName()) == null || forceUpdate) {
                if (!QAPluginAPI.uploadIntent(qCurrentRank)) {
                    warning("Unable to upload intent!");
                }
            }
            if (QAPluginAPI.findIntentByName(qCurrentRankCost.getName()) == null || forceUpdate) {
                if (!QAPluginAPI.uploadIntent(qCurrentRankCost)) {
                    warning("Unable to upload intent!");
                }
            }
            if (QAPluginAPI.findIntentByName(qRankOtherPlayer.getName()) == null || forceUpdate) {
                if (!QAPluginAPI.uploadIntent(qRankOtherPlayer)) {
                    warning("Unable to upload intent!");
                }
            }
            if (QAPluginAPI.findIntentByName(qCurrentRankup.getName()) == null || forceUpdate) {
                if (!QAPluginAPI.uploadIntent(qCurrentRankup)) {
                    warning("Unable to upload intent!");
                }
            }
            if (QAPluginAPI.findIntentByName(qLastRank.getName()) == null || forceUpdate) {
                if (!QAPluginAPI.uploadIntent(qLastRank)) {
                    warning("Unable to upload intent!");
                }
            }
            if (QAPluginAPI.findIntentByName(qLastRankMe.getName()) == null || forceUpdate) {
                if (!QAPluginAPI.uploadIntent(qLastRankMe)) {
                    warning("Unable to upload intent!");
                }
            }
            if (QAPluginAPI.findIntentByName(qRankPrefix.getName()) == null || forceUpdate) {
                if (!QAPluginAPI.uploadIntent(qRankPrefix)) {
                    warning("Unable to upload intent!");
                }
            }
            if (QAPluginAPI.findIntentByName(qRankupCost.getName()) == null || forceUpdate) {
                if (!QAPluginAPI.uploadIntent(qRankupCost)) {
                    warning("Unable to upload intent!");
                }
            }
            if (QAPluginAPI.findIntentByName(qRankupRemaining.getName()) == null || forceUpdate) {
                if (!QAPluginAPI.uploadIntent(qRankupRemaining)) {
                    warning("Unable to upload intent!");
                }
            }
            if (QAPluginAPI.findIntentByName(qCurrentRankCost.getName()) == null || forceUpdate) {
                if (!QAPluginAPI.uploadIntent(qCurrentRankCost)) {
                    warning("Unable to upload intent!");
                }
            }
        } catch (FeatureNotEnabled ex) {
            severe("You do not have a developer access token in your QAPlugin config!");
        }

    }

    /**
     * Possible questions
     */
    public enum QuestionType {
        RANK_CURRENT, IS_LAST_ME, IS_LAST_OTHER, RANK_LAST, RANK_NEXT, RANK_NEXT_COST, RANK_NEXT_REMAINING, RANK_PREFIX, RANK_COST, RANK_OTHERPLAYER,
        RANK_OTHERPLAYER_COST
    }

    /**
     * This method is triggered
     *
     * @param event AIQuestion event containing data you will need
     * @return response string
     */
    public String getResponse(AIQuestionEvent event) {
        Player player = event.getPlayer();
        String defaultResponse = event.getDefaultResponse();

        Map<String, String> params = event.getParameters();
        if (!params.containsKey("question")) {
            return defaultResponse;
        }

        // Get the question type from the parameter
        QuestionType questionType = QuestionType.valueOf(params.get("question").toUpperCase());
        switch (questionType) {
            case RANK_CURRENT:
                String rank = getCurrentRank(player);
                if (rank.equals("")) {
                    // Error - return response
                    return getRandomErrorResponse("rank_current-no-rank", new HashMap<String, String>(), player);
                } else {
                    // Get the response we configured in the intent and return the rank
                    return defaultResponse.replace("$rank", rank);
                }
            case IS_LAST_ME:
                boolean last = isLastRank(player);
                return getRandomResponse("is_last_me-" + String.valueOf(last), new HashMap<String, String>(), player);
            case RANK_PREFIX:
                String prefix = getRankPrefix(player);
                return defaultResponse.replace("$prefix", prefix);
            case RANK_NEXT_COST:
                if (isLastRank(player)) {
                    // Last rank
                    // Error - return response
                    return getRandomErrorResponse("rank_next_cost-last", new HashMap<String, String>(), player);
                } else {
                    String cost = getRankupCost(player);
                    return defaultResponse.replace("$money", cost);
                }
            case RANK_NEXT_REMAINING:
                if (isLastRank(player)) {
                    // Last rank
                    // Error - return response
                    return getRandomErrorResponse("rank_next_remaining-last", new HashMap<String, String>(), player);
                } else {
                    String cost = getRankupCost(player);
                    return defaultResponse.replace("$money", cost);
                }
            case RANK_LAST:
                LastRank lastRank = getLastRank();
                return defaultResponse.replace("lastrank", lastRank.getRank());
            case RANK_COST:
                String currentRankupCost = getCurrentRankCost(player);
                if (currentRankupCost == null) {
                    // Error
                    return getRandomErrorResponse("rank_cost-no-rank", new HashMap<String, String>(), player);
                } else {
                    return defaultResponse.replace("money", currentRankupCost);
                }
            case RANK_OTHERPLAYER:
                if (params.containsKey("player")){
                    // Try to see if it is a real player
                    OfflinePlayer otherPlayer = Bukkit.getPlayer(params.get("player"));
                    if (otherPlayer == null){
                        // Not found
                        return getRandomErrorResponse("rank_otherplayer-no-player", new HashMap<String, String>(), player);
                    }else{
                        // Check if online
                    }
                }
        }

        return null;
    }

    /**
     * Get the current rank
     *
     * @param player player to get rank from
     */
    private String getCurrentRank(Player player) {
        EZAPI api = EZRanksPro.getAPI();
        return api.getCurrentRank(player);
    }

    /**
     * Get the current rank cost
     *
     * @param player player to get rank from
     */
    private String getCurrentRankCost(Player player) {
        Rankup rankup = Rankup.getRankup(player);
        if (rankup == null) {
            return null;
        } else {
            return rankup.getCostString();
        }
    }

    /**
     * Check if you are on the last rank
     *
     * @param player player to check
     * @return last rank
     */
    public boolean isLastRank(Player player) {
        EZAPI api = EZRanksPro.getAPI();
        return api.isLastRank(player);
    }

    /**
     * Get the last rank
     *
     * @return last rank
     */
    public LastRank getLastRank() {
        EZAPI api = EZRanksPro.getAPI();
        return api.getLastRank();
    }

    /**
     * Get rank prefix
     *
     * @param player player
     * @return rank prefix
     */
    public String getRankPrefix(Player player) {
        EZAPI api = EZRanksPro.getAPI();
        return api.getRankPrefix(player);
    }

    /**
     * Get rankup cost
     *
     * @param player player
     * @return cost formatted
     */
    public String getRankupCost(Player player) {
        EZAPI api = EZRanksPro.getAPI();
        return api.getRankupCostFormatted(player);
    }

    /***
     * Get rankup difference
     * @param player player
     * @return rankup difference
     */
    public String getRankupDifference(Player player) {
        Rankup r = Rankup.getRankup(player);
        EZAPI api = EZRanksPro.getAPI();
        double difff = 0.0;
        if (r != null) {
            difff = r.getCost();
            difff = CostHandler.getMultiplier(player, difff);
            difff = CostHandler.getDiscount(player, difff);
        }
        return EcoUtil.fixMoney(EcoUtil.getDifference(
                api.getEconBalance(player), difff));
    }

    /**
     * Get all rankups
     *
     * @return list of rankups
     */
    public List<Rankup> getAllRankups() {
        List<Rankup> rankups = new ArrayList<Rankup>();
        for (Map.Entry<Integer, Rankup> rankupEntry : Rankup.getAllRankups().entrySet()) {
            rankups.add(rankupEntry.getValue());
        }
        return rankups;
    }
}

package be.maximvdw.qaplugin.modules;

import be.maximvdw.qaplugin.api.AIModule;
import be.maximvdw.qaplugin.api.AIQuestionEvent;
import be.maximvdw.qaplugin.api.QAPluginAPI;
import be.maximvdw.qaplugin.api.ai.Context;
import be.maximvdw.qaplugin.api.ai.Intent;
import be.maximvdw.qaplugin.api.ai.IntentResponse;
import be.maximvdw.qaplugin.api.exceptions.FeatureNotEnabled;
import me.clip.ezrankspro.EZAPI;
import me.clip.ezrankspro.EZRanksPro;
import me.clip.ezrankspro.rankdata.LastRank;
import org.bukkit.entity.Player;

import java.util.HashMap;
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

        // Entity


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
                        .addAffectedContext(new Context("ezrankspro", 1))
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
                        .addAffectedContext(new Context("ezrankspro", 1))
                        .addParameter(new IntentResponse.ResponseParameter("question", QuestionType.RANK_NEXT.name())));


        // Question to ask how much it costs for a next rank
        Intent qRankupCost = new Intent("QAPlugin-module-ezrankspro-rank.next.cost")
                .addTemplate("how much does it cost to rankup?")
                .addTemplate("how much does my next rank cost?")
                .addTemplate("how much does it totally cost to rankup?")
                .addResponse(new IntentResponse()
                        .withAction(this)
                        .addAffectedContext(new Context("ezrankspro", 1))
                        .addParameter(new IntentResponse.ResponseParameter("question", QuestionType.RANK_NEXT_COST.name())));


        // Question to ask how much money you need for the next rank
        Intent qRankupRemaining = new Intent("QAPlugin-module-ezrankspro-rank.next.remaining")
                .addTemplate("how much do I need to rankup?")
                .addTemplate("how much money do I need to rankup?")
                .addTemplate("how much money do I still need for my next rank?")
                .addResponse(new IntentResponse()
                        .withAction(this)
                        .addAffectedContext(new Context("ezrankspro", 1))
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
                        .addAffectedContext(new Context("ezrankspro", 1))
                        .addParameter(new IntentResponse.ResponseParameter("question", QuestionType.RANK_PREFIX.name()))
                        .addMessage(new IntentResponse.TextResponse()
                                .addSpeechText("The prefix of your rank is $prefix")
                                .addSpeechText("Your rank uses $prefix as the prefix")
                                .addSpeechText("$prefix is your prefix!")));

        try {
            // Upload the entities
//            if (!QAPluginAPI.uploadEntity(pluginAuthorsEntity)) {
//                warning("Unable to upload entity!");
//            }
//            if (!QAPluginAPI.uploadEntity(pluginsEntity)) {
//                warning("Unable to upload entity!");
//            }

            // Upload the intents
            if (!QAPluginAPI.uploadIntent(qCurrentRank)) {
                warning("Unable to upload intent!");
            }
            if (!QAPluginAPI.uploadIntent(qCurrentRankup)) {
                warning("Unable to upload intent!");
            }
            if (!QAPluginAPI.uploadIntent(qLastRank)) {
                warning("Unable to upload intent!");
            }
            if (!QAPluginAPI.uploadIntent(qLastRankMe)) {
                warning("Unable to upload intent!");
            }
            if (!QAPluginAPI.uploadIntent(qRankPrefix)) {
                warning("Unable to upload intent!");
            }
            if (!QAPluginAPI.uploadIntent(qRankupCost)) {
                warning("Unable to upload intent!");
            }
            if (!QAPluginAPI.uploadIntent(qRankupRemaining)) {
                warning("Unable to upload intent!");
            }
        } catch (FeatureNotEnabled ex) {
            severe("You do not have a developer access token in your QAPlugin config!");
        }

    }

    /**
     * Possible questions
     */
    public enum QuestionType {
        RANK_CURRENT, IS_LAST_ME, IS_LAST_OTHER, RANK_LAST, RANK_NEXT, RANK_NEXT_COST, RANK_NEXT_REMAINING, RANK_PREFIX
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
                } else {
                    String cost = getRankupCost(player);
                    return defaultResponse.replace("$money", cost);
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
}

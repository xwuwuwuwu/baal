package com.lab.azure.bootloader.utils;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.internal.DefaultConverterFactory;
import com.lab.azure.bootloader.utils.arguments.DeployArgument;
import com.lab.azure.bootloader.utils.arguments.PlanArgument;
import com.lab.azure.bootloader.utils.arguments.QueryArgument;
import com.microsoft.azure.storage.StorageException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.UUID;


/**
 * @author willsun
 */
public class Program {


    public static void main(String[] args) throws URISyntaxException, StorageException, InvalidKeyException, IOException {
        PlanArgument planArgument = new PlanArgument();
        DeployArgument deployArgument = new DeployArgument();
        QueryArgument queryArgument = new QueryArgument();
        JCommander jCommander = JCommander.newBuilder()
            .addConverterFactory(new DefaultConverterFactory())
            .addCommand("plan", planArgument)
            .addCommand("deploy", deployArgument)
            .addCommand("query", queryArgument)
            .build();
        jCommander.parse(args);

        switch (jCommander.getParsedCommand().toLowerCase()) {
            case "plan":
                PlanHelper.makePlan(planArgument);
                break;
            case "deploy":
                doDeploy(deployArgument);
                break;
            case "query":
                doQuery(queryArgument);
                break;
        }
    }



    private static void doDeploy(DeployArgument deployArgument) throws IOException, URISyntaxException, StorageException, InvalidKeyException {
        DeployHelper.deploy(deployArgument);
    }

    private static void doQuery(QueryArgument queryArgument) throws IOException, StorageException, InvalidKeyException, URISyntaxException {
        QueryHelper.queryJob(queryArgument);
    }




    private static boolean checkKey(String keyString) {
        try {
            UUID uuid = UUID.fromString(keyString);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

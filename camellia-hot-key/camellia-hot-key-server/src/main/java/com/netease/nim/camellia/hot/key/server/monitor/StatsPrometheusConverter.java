package com.netease.nim.camellia.hot.key.server.monitor;

/**
 * Created by caojiajun on 2023/5/11
 */
public class StatsPrometheusConverter {

    public static String converter(HotKeyServerStats serverStats) {
        StringBuilder builder = new StringBuilder();
        builder.append("# HELP hot_key_server_connect_count Client Connect Count\n");
        builder.append("# TYPE hot_key_server_connect_count gauge\n");
        builder.append("hot_key_server_connect_count ").append(serverStats.getConnectCount()).append("\n");

        builder.append("# HELP hot_key_server_work_thread Work Thread\n");
        builder.append("# TYPE hot_key_server_work_thread gauge\n");
        builder.append("hot_key_server_work_thread ").append(serverStats.getQueueStats().getQueueNum()).append("\n");

        builder.append("# HELP hot_key_server_work_queue_pending Work Queue Pending\n");
        builder.append("# TYPE hot_key_server_work_queue_pending gauge\n");
        String workQueuePendingFormat = "hot_key_server_work_queue_pending{queue=\"%s\",} %d\n";
        for (QueueStats.Stats stats : serverStats.getQueueStats().getStatsList()) {
            builder.append(String.format(workQueuePendingFormat, "queue-" + stats.getId(), stats.getPendingSize()));
        }

        builder.append("# HELP hot_key_server_work_queue_discard Work Queue Discard\n");
        builder.append("# TYPE hot_key_server_work_queue_discard gauge\n");
        String workQueueDiscardFormat = "hot_key_server_work_queue_discard{queue=\"%s\",} %d\n";
        for (QueueStats.Stats stats : serverStats.getQueueStats().getStatsList()) {
            builder.append(String.format(workQueueDiscardFormat, "queue-" + stats.getId(), stats.getDiscardCount()));
        }

        builder.append("# HELP hot_key_server_traffic_total Traffic Total\n");
        builder.append("# TYPE hot_key_server_traffic_total gauge\n");
        TrafficStats trafficStats = serverStats.getTrafficStats();
        builder.append("hot_key_server_traffic_total ").append(trafficStats.getTotal()).append("\n");

        builder.append("# HELP hot_key_server_traffic_detail Traffic Detail\n");
        builder.append("# TYPE hot_key_server_traffic_detail gauge\n");
        String trafficDetailFormat = "hot_key_server_traffic_detail{namespace=\"%s\",type=\"%s\",} %d\n";
        for (TrafficStats.Stats stats : trafficStats.getStatsList()) {
            builder.append(String.format(trafficDetailFormat, stats.getNamespace(), stats.getType(), stats.getCount()));
        }

        return builder.toString();
    }
}

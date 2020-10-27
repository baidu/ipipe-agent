appender("AGENT", FileAppender, {
    file = "agent.log"
    encoder(PatternLayoutEncoder) {
        pattern = "%d [%thread] %-5level %logger{36} %line - %msg%n"
    }
})

def agentUpgradeLog = System.getProperty("user.home") + File.separator + ".agent.upgrade.log";
appender("UPGRADE_LOG", FileAppender, {
    file = "${agentUpgradeLog}"
    encoder(PatternLayoutEncoder) {
        pattern = "%d [%thread] %-5level %logger{36} %line - %msg%n"
    }
})
logger("UPGRADE_LOG", DEBUG, ["UPGRADE_LOG"], false)
root(INFO, ["AGENT"])
#!/usr/bin/env groovy

/**
 * Send a slack notifications if the build status has changed
 */
def call(def buildStandalone = false)
{
	String buildStatus = currentBuild.currentResult ?: 'SUCCESS'
	String generalizedStatus = generalizeBuildStatus(currentBuild)
	String generalizedPrevStatus = generalizeBuildStatus(currentBuild.previousBuild)

	// From here on we only need to handle: SUCCESS, UNSTABLE, FAILURE
	boolean successful = (generalizedStatus == 'SUCCESS')
	boolean failed = (generalizedStatus == 'FAILURE')
	boolean backToNormal = (generalizedStatus == 'SUCCESS' && generalizedPrevStatus != 'SUCCESS')
	boolean unchanged = (generalizedStatus == generalizedPrevStatus)

	// 'non-standalone' builds:
	// do not report subsequent same results
	// only report success if back to normal (covered by above requirement)
	//
	// 'standalone' builds:
	// only report failures, unstable is used for 'expected' failure states
	// (eg: web triggers a build even if the program upload failed, so there
	// is no program to download and also not shown on WEB)
	// report subsequent failures to get aware of longstanding broken builds
	if ((!buildStandalone && unchanged) || (buildStandalone && !failed)) {
		return
	}

	// Set text
	def buildStatusText = buildStatus
	if (backToNormal) {
		buildStatusText = "${buildStatus} (Back to Normal)"
	}
	def message = "${buildStatusText}: '${env.JOB_NAME} [${env.BUILD_DISPLAY_NAME}]' (<${env.BUILD_URL}|Open>)"

	// Set color
	def color = 'good'
	if (generalizedStatus == 'SUCCESS') {
		color = 'good'
	} else if (generalizedStatus == 'UNSTABLE') {
		color = 'warning'
	} else {
		color = 'danger'
	}

	// Set channel
	def channel = "#ci-status"
	if (buildStandalone) {
		channel = "${channel},#ci-status-standalone"
	}

	// Send notifications
	echo "Send to Slack: [${channel}] ${color}: ${message}"
	slackSend(color: color, message: message, channel: channel)
}

/**
 * Maps the build status to fewer statuses (SUCCESS, UNSTABLE, FAILURE) for simplicity.
 */
String generalizeBuildStatus(def build)
{
	def statusMapping = ['ABORTED': 'FAILURE', 'NOT_BUILT': 'SUCCESS']
	return statusMapping.get(build?.currentResult, build?.currentResult ?: 'SUCCESS')
}

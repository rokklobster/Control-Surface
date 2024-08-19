(ns control-surface.constants)

(defonce help "")
(defonce schedule-help
  "This command requires 3 arguments: target server, task name, and command string.
   Example: `my-server my-task ls -la .`
   Server name and task name should not contain spaces; command, on the other hand, can.
   Command is split to arguments as shell would split it - use quotes and double quotes to prevent: `srv tsk cat 'my file.txt' | grep \"string to search for\"`.")

(defonce query-tasks-help
  "The command requires server name. Example: `/query my-server`")

(defn query-server-missing [name]
  (str "Server" name "not found"))

(defonce schedule-ok
  "Command is sent to the client.")

(defonce schedule-server-missing
  "Requested server is not registered.")

(defn successful-registration [name]
  (str "New server registered: " name ".\nIf you don't recognize the server, void your token and reissue it."))

(defn failed-registration [name] 
  (str "Failed to register server " name ".\nIf you don't recognize the server, void and then reissue your token."))

(defonce cancel-help
  "Cancel command requires server name and task name. Example: `/cancel server task`")

(defn cancel-missing-server [name]
  (str "Server " name " not found"))

(defonce cancel-processed
  "Cancellation sent. Process may send some output before complete unload.")
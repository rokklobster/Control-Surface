(ns control-surface.models)

(defrecord Options [botToken port])

(defrecord TaskRunRequest
           [taskName
            taskCommand
            taskType
            scheduledAt
            targetServer])

(defrecord TaskCancelRequest
           [taskName targetServer])

(defrecord TaskOutput
           [taskName
            timestamp
            text])

(defrecord ServerRegistrationRequest
           [serverName pushUrl cancelUrl queryUrl])

(defrecord ServerUnregisterRequest [serverName])
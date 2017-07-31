(ns metabase.sync.analyze.table-row-count
  "Logic for updating a Table's row count by running appropriate MBQL queries."
  (:require [clojure.tools.logging :as log]
            [metabase.db.metadata-queries :as queries]
            [metabase.models.table :refer [Table]]
            [metabase.sync
             [interface :as i]
             [util :as sync-util]]
            [metabase.util :as u]
            [schema.core :as s]
            [toucan.db :as db]))

(s/defn ^:private ^:always-validate table-row-count :- (s/maybe s/Int)
  "Determine the count of rows in TABLE by running a simple structured MBQL query."
  [table :- i/TableInstance]
  (sync-util/with-error-handling (format "Unable to determine row count for %s" (sync-util/name-for-logging table))
    (queries/table-row-count table)))

(s/defn ^:always-validate update-row-count-for-table!
  "Update the cached row count (`rows`) for a single TABLE."
  [table :- i/TableInstance]
  (sync-util/with-error-handling (format "Error setting table row count for %s" (sync-util/name-for-logging table))
    (when-let [row-count (table-row-count table)]
      (log/debug (format "Set table row count for %s to %d" (sync-util/name-for-logging table) row-count))
      (db/update! Table (u/get-id table)
        :rows row-count))))

(s/defn ^:always-validate update-table-row-counts!
  "Update the cached row count (`rows`) for all the syncable tables in DATABASE."
  [database :- i/DatabaseInstance]
  (doseq [table (sync-util/db->sync-tables database)]
    (update-row-count-for-table! table)))
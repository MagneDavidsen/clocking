(ns clocking.client.repl
  (:require [clojure.browser.repl :as repl]))
(repl/connect "http://localhost:9000/repl")

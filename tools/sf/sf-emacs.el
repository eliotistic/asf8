;;; sf-emacs.el --- send an ABC buffer to the String Fingering window  -*- lexical-binding: t; -*-
;;
;; Right-click in an ABC buffer -> "Send to String Fingering" (entry in shape/emacs/right-click-menu.el).  Or M-x sf-send-abc.
;;
;; Saves the buffer, then runs  tools/sf/sfctl.py gui_load_abc path=FILE  against the window that
;; wrote ~/.string_fingering/control.port.  If no window is running, starts one (string-fingering.sh)
;; and waits for it.  Errors are messages in the echo area; nothing else happens in Emacs.
;;
;; Load:  (load "~/shape/fingering/tools/sf/sf-emacs.el")

(defvar sf-app-dir (expand-file-name "~/shape/fingering"))
(defvar sf-control-port-file (expand-file-name "~/.string_fingering/control.port"))
(defvar sf-start-wait-seconds 40)

(defun sf--port-ready-p ()
  "True when a window has written its control port and that port answers."
  (and (file-exists-p sf-control-port-file)
       (zerop (call-process "python3" nil nil nil "-I"
                            (expand-file-name "tools/sf/sfctl.py" sf-app-dir) "gui_state"))))

(defun sf--ensure-window ()
  "Start the String Fingering window if none answers.  Return non-nil when one does."
  (or (sf--port-ready-p)
      (progn
        (message "Starting String Fingering...")
        (when (file-exists-p sf-control-port-file) ; stale file from a window that has gone
          (rename-file sf-control-port-file (concat sf-control-port-file ".stale") t))
        (start-process "string-fingering" nil
                       (expand-file-name "tools/sf/string-fingering.sh" sf-app-dir))
        (let ((n 0))
          (while (and (< n (* 2 sf-start-wait-seconds)) (not (sf--port-ready-p)))
            (sit-for 0.5)
            (setq n (1+ n))))
        (sf--port-ready-p))))

;; ABCD is not plain ABC: the window's ABC reader cannot read it.  So the file goes through JJ
;; (abc_run, then sq_mei writes the MEI jjviz would give Verovio) and the window loads the MEI.
(defvar sf-jj-command (expand-file-name "~/shape/bin/jj_cmd"))
(defvar sf-mei-dir (expand-file-name "~/.string_fingering/abcd"))

(defun sf--load-in-window (file shown)
  (let* ((out (generate-new-buffer " *sf-send*"))
         (status (call-process "python3" nil out nil "-I"
                               (expand-file-name "tools/sf/sfctl.py" sf-app-dir)
                               "gui_load_abc" (concat "path=" file)))
         (reply (with-current-buffer out (buffer-string)))
         (items (and (string-match "\"items\":\\([0-9]+\\)" reply)
                     (string-to-number (match-string 1 reply)))))
    (kill-buffer out)
    (message "String Fingering: %s"
             (cond ((not (zerop status)) (string-trim reply))
                   ((and items (zerop items)) (format "%s was opened but has no notes (items 0)" shown))
                   (t (format "loaded %s (%s items)" shown (or items "?")))))))

;;;###autoload
(defun sf-send-abc ()
  "Save this ABCD buffer, convert it through JJ to MEI, and load that into the String Fingering window."
  (interactive)
  (unless buffer-file-name
    (user-error "This buffer has no file; save it first"))
  (save-buffer)
  (make-directory sf-mei-dir t)
  (let* ((src (expand-file-name buffer-file-name))
         (name (file-name-nondirectory src))
         (mei (expand-file-name (concat (file-name-sans-extension name) ".mei") sf-mei-dir))
         (log (generate-new-buffer " *sf-jj*"))
         (process-environment
          (cons (concat "PATH=" (expand-file-name "tools/jj/nojump" sf-app-dir) ":" (getenv "PATH"))
                process-environment)))
    (when (file-exists-p mei) (rename-file mei (concat mei ".prev") t))
    (message "String Fingering: reading %s through JJ ..." name)
    (let ((proc (make-process
                 :name "sf-jj" :buffer log :command (list sf-jj-command "--stdio") :noquery t
                 :sentinel
                 (lambda (p _event)
                   (unless (process-live-p p)
                     (if (file-exists-p mei)
                         (if (sf--ensure-window)
                             (sf--load-in-window mei name)
                           (message "String Fingering did not start (see ~/.string_fingering/sf.log)"))
                       (message "String Fingering: JJ made no MEI from %s: %s" name
                                (with-current-buffer log
                                  (let ((tail (buffer-substring (max (point-min) (- (point-max) 400)) (point-max))))
                                    (string-trim tail)))))
                     (kill-buffer log))))))
      (process-send-string proc (format "S = abc_run(\"%s\")\nsq_mei(S, \"%s\")\n" src mei))
      (process-send-eof proc))))

;; The right-click entry itself lives in ~/shape/emacs/right-click-menu.el (ABCD section).
(defun sf-install-in-buffer ()
  (local-set-key (kbd "C-c C-f") #'sf-send-abc))

(dolist (hook '(abcd-mode-hook abc-mode-hook))
  (add-hook hook #'sf-install-in-buffer))
;; Buffers that are already open
(dolist (b (buffer-list))
  (with-current-buffer b
    (when (memq major-mode '(abcd-mode abc-mode)) (sf-install-in-buffer))))

(provide 'sf-emacs)
;;; sf-emacs.el ends here

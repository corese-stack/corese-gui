.. image:: _static/logo/corese-gui-logo.png
   :align: center
   :width: 230px
   :class: homepage-brand-logo

.. raw:: html

   <h1 class="homepage-brand-title">Corese-GUI</h1>

Corese-GUI is the desktop application of the Corese Semantic Web stack.
It provides a visual workspace to load RDF data, run SPARQL queries, inspect results, validate data with SHACL, and reason with rules.

From the 5.x line, Corese-GUI has been rebuilt with JavaFX and a modular architecture.
The interface is organized around dedicated Data, Query, Validation, Logs, and Settings views.

.. grid:: 2
   :gutter: 2

   .. grid-item::

      .. raw:: html

         <div class="theme-screenshot">
           <img class="theme-screenshot-light" src="_static/screenshots/data-graph-exploration.png" alt="Inspect named graphs and graph components in the data workspace.">
           <img class="theme-screenshot-dark" src="_static/screenshots/data-graph-exploration-dark.png" alt="Inspect named graphs and graph components in the data workspace.">
         </div>

   .. grid-item::

      .. raw:: html

         <div class="theme-screenshot">
           <img class="theme-screenshot-light" src="_static/screenshots/data-reasoning-custom-rules.png" alt="Reasoning workspace with built-in profiles and custom rule files.">
           <img class="theme-screenshot-dark" src="_static/screenshots/data-reasoning-custom-rules-dark.png" alt="Reasoning workspace with built-in profiles and custom rule files.">
         </div>

   .. grid-item::

      .. raw:: html

         <div class="theme-screenshot">
           <img class="theme-screenshot-light" src="_static/screenshots/query-template-dialog.png" alt="SPARQL query template dialog.">
           <img class="theme-screenshot-dark" src="_static/screenshots/query-template-dialog-dark.png" alt="SPARQL query template dialog.">
         </div>

   .. grid-item::

      .. raw:: html

         <div class="theme-screenshot">
           <img class="theme-screenshot-light" src="_static/screenshots/query-select-table-results.png" alt="SPARQL SELECT results displayed in the table view.">
           <img class="theme-screenshot-dark" src="_static/screenshots/query-select-table-results-dark.png" alt="SPARQL SELECT results displayed in the table view.">
         </div>

   .. grid-item::

      .. raw:: html

         <div class="theme-screenshot">
           <img class="theme-screenshot-light" src="_static/screenshots/query-construct-graph-zoomed.png" alt="Graph exploration after a SPARQL CONSTRUCT query.">
           <img class="theme-screenshot-dark" src="_static/screenshots/query-construct-graph-zoomed-dark.png" alt="Graph exploration after a SPARQL CONSTRUCT query.">
         </div>

   .. grid-item::

      .. raw:: html

         <div class="theme-screenshot">
           <img class="theme-screenshot-light" src="_static/screenshots/validation-shacl-editor.png" alt="SHACL shapes editor in the validation workspace.">
           <img class="theme-screenshot-dark" src="_static/screenshots/validation-shacl-editor-dark.png" alt="SHACL shapes editor in the validation workspace.">
         </div>

.. raw:: html

   <h3>Corese ecosystem</h3>

* `corese-core <https://corese-stack.github.io/corese-core/>`_: Java library for RDF processing and reasoning.
* `corese-command <https://corese-stack.github.io/corese-command/>`_: command line interface for Corese features.
* `corese-server <https://corese-stack.github.io/corese-server-jetty/>`_: SPARQL endpoint server and management utilities.
* `corese-python (beta) <https://corese-stack.github.io/corese-python/>`_: Python integration around Corese APIs.

.. raw:: html

   <h3>Contributions and discussions</h3>

.. _discussion forum: https://github.com/orgs/corese-stack/discussions
.. _issue reports: https://github.com/corese-stack/corese-gui/issues
.. _pull requests: https://github.com/corese-stack/corese-gui/pulls

For questions or feedback, use the `discussion forum`_.
To report bugs or propose changes, open `issue reports`_ and `pull requests`_.

.. raw:: html

   <div style="visibility: hidden;">

Home Page
===================================

.. raw:: html

   </div>

.. toctree::
   :hidden:

   Downloads <install>

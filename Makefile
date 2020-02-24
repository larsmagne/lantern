build:
	lein do clean, cljsbuild once release

deploy-web:
	(cd public; rsync -avR css js/app.js index.html www@quimby:html/circus/lantern/)

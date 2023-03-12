build:
	lein do clean, cljsbuild once release

deploy-web:
	(cd public; rsync -avR css js/app.js index.html *.js www@quimby:html/circus/lantern/)

deploy-images:
	rsync -av /home/larsi/lanterne/small/ www@quimby:html/circus/lanterne/
	rsync -av /home/larsi/lanterne/tiny www@quimby:html/circus/lanterne/
	rsync -av /home/larsi/lanterne/pages www@quimby:html/circus/lanterne/

run-server:
	lein figwheel

make-tiny:
	make-images

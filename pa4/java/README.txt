The way to run the baseline model:

java -Xmx1g -cp "classes:extlib/*" cs224n.deep.NER ../data/train ../data/dev -print baseline

The way to run the window model:

java -Xmx1g -cp "classes:extlib/*" cs224n.deep.NER ../data/train ../data/dev -print 
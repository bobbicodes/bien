(deftest test-128
  (is (= {:suit :diamond :rank 10} (cards "DQ")))
  (is (= {:suit :heart :rank 3} (cards "H5")))
  (is (= {:suit :club :rank 12} (cards "CA")))
  (is (= (range 13) (map (comp :rank cards str)
                         '[S2 S3 S4 S5 S6 S7
                           S8 S9 ST SJ SQ SK SA]))))
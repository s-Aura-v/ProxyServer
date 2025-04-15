import subprocess

# Input string to send to the Java program
input_lines = """
yes
https://s-aura-v.com/assets/S24_P2_3-DHpYVaBM.png
https://www.seacoastonline.com/gcdn/presto/2021/11/29/NPOH/e28ea279-030e-4c1d-a574-dffd5da55e48-gray_squirrel_cropped.jpg
https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcRB_PC93m0bHPZUpGZ1yen5N6-9dRT0Se_18A&s
https://www.seacoastonline.com/gcdn/presto/2021/11/29/NPOH/e28ea279-030e-4c1d-a574-dffd5da55e48-gray_squirrel_cropped.jpg
https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Elizabeth_Tower%2C_June_2022.jpg/640px-Elizabeth_Tower%2C_June_2022.jpg
https://www.bushheritage.org.au/uploads/main/Images/Places/Qld/pilungah/RS32891-kanga-pilung-peter-wallis.jpg
https://upload.wikimedia.org/wikipedia/commons/thumb/8/84/Male_and_female_chicken_sitting_together.jpg/800px-Male_and_female_chicken_sitting_together.jpg
https://dynamic-media-cdn.tripadvisor.com/media/photo-o/2b/32/05/d4/the-stunning-scissor.jpg
https://m.media-amazon.com/images/I/815gN5NqNcL._AC_UF894,1000_QL80_.jpg
https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Elizabeth_Tower%2C_June_2022.jpg/640px-Elizabeth_Tower%2C_June_2022.jpg
https://i.ytimg.com/vi/2DjGg77iz-A/sddefault.jpg
https://s-aura-v.com/assets/F24_P1-C98K-uUV.png
https://s-aura-v.com/assets/S24_P2_1-CTsGpXsh.png
https://s-aura-v.com/assets/S24_P2_2-Cz83HeVL.png
https://upload.wikimedia.org/wikipedia/commons/thumb/4/43/Elizabeth_Tower%2C_June_2022.jpg/640px-Elizabeth_Tower%2C_June_2022.jpg
https://s-aura-v.com/assets/S24_P2_3-DHpYVaBM.png
https://s-aura-v.com/assets/S24_P2_3-DHpYVaBM.png
https://www.peta.org/wp-content/uploads/2011/08/baby-chicks.jpg
https://cdn.prod.website-files.com/5eae38112aed416159affd28/5ebaf4ae8d21df73a44d5a23_19.jpg
https://m.media-amazon.com/images/I/815gN5NqNcL._AC_UF894,1000_QL80_.jpg
https://m.media-amazon.com/images/I/815gN5NqNcL._AC_UF894,1000_QL80_.jpg
https://cdn.eso.org/images/wallpaper5/eso1907a.jpg
https://i.dailymail.co.uk/1s/2019/12/19/18/22464590-7810929-image-m-13_1576780377144.jpg
https://giraffeconservation.org/wp-content/uploads/2024/11/featured-16-9_southern-3-topaz.jpg
https://giraffeconservation.org/wp-content/uploads/2024/11/featured-16-9_southern-3-topaz.jpg
https://i.ytimg.com/vi/2DjGg77iz-A/sddefault.jpg
https://www.oswego.edu/news/sites/www.oswego.edu.news/files/styles/panopoly_image_original/public/healthcareintelligence.jpg
https://s-aura-v.com/assets/S24_P2_3-DHpYVaBM.png
https://cdn.prod.website-files.com/5eae38112aed416159affd28/5ebaf4ae8d21df73a44d5a23_19.jpg
https://s-aura-v.com/assets/F24_P1-C98K-uUV.png
exit
""".strip()

# Replace this with your actual JAR file
jar_file = "target/Client.jar"

# Java command
cmd = ["java", "-jar", jar_file]

# Run the process and feed input
try:
    result = subprocess.run(
        cmd,
        input=input_lines,
        capture_output=True,
        text=True
    )
    print("=== Java Output ===")
    print(result.stdout)
    if result.stderr:
        print("=== Java Errors ===")
        print(result.stderr)
except Exception as e:
    print("Failed to run Java program:", e)

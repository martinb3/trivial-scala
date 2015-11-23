# trivial-scala
A slack bot for Trivia written in Scala

To submit a quiz, fork the repo and add a .json quiz file to:
trivial-scala/src/main/resources

Then issue pull request to:
https://github.com/martinb3/trivial-scala

Sample quiz formatting:
{
  "title": "QUIZ-LING!",
  "ref": "http://www.funtrivia.com/",
  "order": "seq",
  "questions" : [
     {
      "type": "image",
      "text": "http://8b7621bceac5dd360860-252673abe58ab44b12c31463fdecf09f.r83.cf4.rackcdn.com/b8e760bf91bcbbd284185ed981ce1ecd.jpg",
      "answer": [ "oprah", "oprah winfrey" ]
      "explanation": "Oprah Gail Winfrey (born January 29, 1954) is an American media proprietor, talk show host, actress, producer, and philanthropist. Winfrey is best known for her talk show The Oprah Winfrey Show, which was the highest-rated program of its kind in history and was nationally syndicated from 1986 to 2011.",
      "points": 0.99
    },
    {
      "type": "simple",
      "text": "Which non-metallic element has the chemical symbol S?",
      "answer": ["Sulphur", "Sulfur"],
      "explanation": "Elementary! Sodium is Na, Silicon is Si and Selenium is Se. NB: this element is spelt 'sulfur' in the US."
    },
    {
      "type": "simple",
      "text": "In which African country is Timbuktu?",
      "answer": [ "Mali" ]
    }
  ]
}

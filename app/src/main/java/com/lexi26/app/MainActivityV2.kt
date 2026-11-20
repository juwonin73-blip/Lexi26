package com.lexi26.app

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color

class MainActivityV2 : Activity() {
    private data class Card(val language: String, val word: String, val meaning: String)
    private val cards = listOf(
        Card("영어", "ephemeral", "덧없는, 일시적인"),
        Card("영어", "ubiquitous", "어디에나 있는"),
        Card("영어", "ambiguous", "모호한"),
        Card("프랑스어", "éphémère", "덧없는"),
        Card("프랑스어", "pourtant", "그럼에도 불구하고")
    )
    private var index = 0
    private var revealed = false
    private lateinit var cardText: TextView
    private lateinit var progressText: TextView
    private lateinit var revealButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(20))
            setBackgroundColor(Color.rgb(247, 247, 251))
        }
        root.addView(TextView(this).apply {
            text = "Lexi26"
            textSize = 30f
            setTextColor(Color.rgb(81, 70, 216))
            setTypeface(null, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "단어 학습을 다시, 단단하게 시작합니다."
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(5), 0, dp(22))
        })
        progressText = TextView(this).apply {
            textSize = 14f
            setTextColor(Color.GRAY)
        }
        root.addView(progressText)
        cardText = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 27f
            setTextColor(Color.rgb(38, 36, 67))
            setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.WHITE)
            setPadding(dp(18), dp(36), dp(18), dp(36))
        }
        root.addView(cardText, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(230)
        ).apply { setMargins(0, dp(14), 0, dp(16)) })

        revealButton = Button(this).apply {
            text = "뜻 보기"
            setOnClickListener {
                revealed = !revealed
                updateCard()
                text = if (revealed) "단어 가리기" else "뜻 보기"
            }
        }
        root.addView(revealButton)
        root.addView(Button(this).apply {
            text = "다음 단어"
            setOnClickListener {
                index = (index + 1) % cards.size
                revealed = false
                revealButton.text = "뜻 보기"
                updateCard()
            }
        })
        root.addView(TextView(this).apply {
            text = "v2 설치 확인용 초기 빌드 · 오프라인 화면"
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0, dp(24), 0, 0)
        })
        setContentView(root)
        updateCard()
    }

    private fun updateCard() {
        val current = cards[index]
        progressText.text = "${current.language}  ·  ${index + 1} / ${cards.size}"
        cardText.text = if (revealed) "${current.word}\n\n${current.meaning}" else "${current.word}\n\n뜻을 떠올려 보세요"
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

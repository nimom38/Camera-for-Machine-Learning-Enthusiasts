package com.example.android.aicamera

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.text.Html
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.example.android.aicamera.databinding.FragmentInfoBinding
import com.google.android.material.snackbar.Snackbar

private const val ARG_PARAM = "type"


class InfoFragment : Fragment() {
    private lateinit var binding: FragmentInfoBinding
    private var param: String? = null
    private lateinit var description: String
    private lateinit var title: String
    private lateinit var direction: NavDirections

    private lateinit var safeContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param = it.getString(ARG_PARAM)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        safeContext = context
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        binding = FragmentInfoBinding.inflate(layoutInflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner

            var isToolbarShown = false

            // scroll change listener begins at Y = 0 when image is fully collapsed
            aicameraDetailScrollview.setOnScrollChangeListener(
                NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->

                    // User scrolled past image to height of toolbar and the title text is
                    // underneath the toolbar, so the toolbar should be shown.
                    val shouldShowToolbar = scrollY > toolbar.height

                    // The new state of the toolbar differs from the previous state; update
                    // appbar and toolbar attributes.
                    if (isToolbarShown != shouldShowToolbar) {
                        isToolbarShown = shouldShowToolbar

                        // Use shadow animator to add elevation if toolbar is shown
                        appbar.isActivated = shouldShowToolbar

                        // Show the plant name if toolbar is shown
                        toolbarLayout.isTitleEnabled = shouldShowToolbar
                    }
                }
            )

            setHasOptionsMenu(true)
            toolbar.setNavigationOnClickListener { view ->
                view.findNavController().navigateUp()
            }

            if(param == "object detection") {
                detailImage.setImageResource(R.drawable.object_detection)
                description = "<p>Object detection is a computer technology related to computer vision and image processing that deals with detecting instances of semantic objects of a certain class (such as humans, buildings, or cars) in digital images and videos. Well-researched domains of object detection include face detection and pedestrian detection. Object detection has applications in many areas of computer vision, including image retrieval and video surveillance. <br/> It is widely used in computer vision tasks such as image annotation, vehicle counting, activity recognition, face detection, face recognition, video object co-segmentation. It is also used in tracking objects, for example tracking a ball during a football match, tracking movement of a cricket bat, or tracking a person in a video. <br/> Every object class has its own special features that helps in classifying the class – for example all circles are round. Object class detection uses these special features. For example, when looking for circles, objects that are at a particular distance from a point (i.e. the center) are sought. Similarly, when looking for squares, objects that are perpendicular at corners and have equal side lengths are needed. A similar approach is used for face identification where eyes, nose, and lips can be found and features like skin color and distance between eyes can be found. <br/> Methods for object detection generally fall into either neural network-based or non-neural approaches. For non-neural approaches, it becomes necessary to first define features using some methods, then using a technique such as support vector machine (SVM) to do the classification. On the other hand, neural techniques are able to do end-to-end object detection without specifically defining features, and are typically based on convolutional neural networks (CNN). <br/> <br/> Source: Wikipedia </p>"
                title = "Object Detection"
                direction = InfoFragmentDirections.actionInfoFragmentToObjCamera()
            }
            else if(param == "image labelling") {
                detailImage.setImageResource(R.drawable.image_classification)
                description = "<p> Our image labelling employs a sort of image classification technique. <br/> The convolutional neural network (CNN) is a class of deep learning neural networks. CNNs represent a huge breakthrough in image recognition. They’re most commonly used to analyze visual imagery and are frequently working behind the scenes in image classification. They can be found at the core of everything from Facebook’s photo tagging to self-driving cars. They’re working hard behind the scenes in everything from healthcare to security <br/><br/> They’re fast and they’re efficient. But how do they work? <br/><br/>Image classification is the process of taking an input (like a picture) and outputting a class (like “cat”) or a probability that the input is a particular class (“there’s a 90% probability that this input is a cat”). You can look at a picture and know that you’re looking at a terrible shot of your own face, but how can a computer learn to do that? <br/><br/> With a convolutional neural network <br/><br/> CNNs have an input layer, and output layer, and hidden layers. The hidden layers usually consist of convolutional layers, ReLU layers, pooling layers, and fully connected layers. <br/><br/> CNNs have an input layer, and output layer, and hidden layers. The hidden layers usually consist of convolutional layers, ReLU layers, pooling layers, and fully connected layers. <br/><br/> Source: medium.com</p>"
                title = "Image Labelling"
                direction = InfoFragmentDirections.actionInfoFragmentToImageLabelling()
            }
            else if(param == "pose estimation") {
                detailImage.setImageResource(R.drawable.pose_estimation)
                description = "<p> In computer vision and robotics, a typical task is to identify specific objects in an image and to determine each object's position and orientation relative to some coordinate system. This information can then be used, for example, to allow a robot to manipulate an object or to avoid moving into the object. The combination of position and orientation is referred to as the pose of an object, even though this concept is sometimes used only to describe the orientation. Exterior orientation and translation are also used as synonyms of pose. <br/> The image data from which the pose of an object is determined can be either a single image, a stereo image pair, or an image sequence where, typically, the camera is moving with a known velocity. The objects which are considered can be rather general, including a living being or body parts, e.g., a head or hands. The methods which are used for determining the pose of an object, however, are usually specific for a class of objects and cannot generally be expected to work well for other types of objects. <br/> Various learning based methods are used for pose estimation. These methods use artificial learning-based system which learn the mapping from 2D image features to pose transformation. In short, this means that a sufficiently large set of images of the object, in different poses, must be presented to the system during a learning phase. Once the learning phase is completed, the system should be able to present an estimate of the object's pose given an image of the object. <br/><br/> Source: Wikipedia </p>"
                title = "Pose Estimation"
                direction = InfoFragmentDirections.actionInfoFragmentToPoseEstimation()
            }
            else if(param == "face detection") {
                detailImage.setImageResource(R.drawable.face_detection)
                description = "<p> Face detection is a computer technology being used in a variety of applications that identifies human faces in digital images. <br/> Face detection can be regarded as a specific case of object-class detection. In object-class detection, the task is to find the locations and sizes of all objects in an image that belong to a given class. Examples include upper torsos, pedestrians, and cars. Face detection simply answers two question, 1. are there any human faces in the collected images or video? 2. where is the located? <br/> Face-detection algorithms focus on the detection of frontal human faces. It is analogous to image detection in which the image of a person is matched bit by bit. Image matches with the image stores in database. Any facial feature changes in the database will invalidate the matching process. <br/> A reliable face-detection approach based on the genetic algorithm and the eigen-face technique: <br/> Firstly, the possible human eye regions are detected by testing all the valley regions in the gray-level image. Then the genetic algorithm is used to generate all the possible face regions which include the eyebrows, the iris, the nostril and the mouth corners. <br/> Each possible face candidate is normalized to reduce both the lighting effect, which is caused by uneven illumination; and the shirring effect, which is due to head movement. The fitness value of each candidate is measured based on its projection on the eigen-faces. After a number of iterations, all the face candidates with a high fitness value are selected for further verification. At this stage, the face symmetry is measured and the existence of the different facial features is verified for each face candidate. <br/> <br/> Source: Wikipedia <p>"
                title = "Face Detection"
                direction = InfoFragmentDirections.actionInfoFragmentToFaceDetection()
            }

            aicameraDescription.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(description, Html.FROM_HTML_MODE_COMPACT)
            } else {
                Html.fromHtml(description)
            }

            fab.setOnClickListener{
                findNavController().navigate(direction)
            }

            aicameraDetailName.text = title

        }
        return binding.root
    }

    override fun onDetach() {
        super.onDetach()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

}